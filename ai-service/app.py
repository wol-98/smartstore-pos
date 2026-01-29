from flask import Flask, request, jsonify
from datetime import datetime, timedelta
import os
import random
import difflib 
import re # 🧹 For cleaning text

app = Flask(__name__)

# ==========================================
# 🧠 1. THE NLP BRAIN (Refined)
# ==========================================

# 🧠 FINAL POLISH: UI Help & Staff Fixes
INTENTS = {
    # 💰 FINANCIALS
    "GET_REVENUE": ["revenue", "sales", "income", "how much money", "cash collected", "daily earnings", "total sales"],
    "GET_PROFIT": ["profit", "margin", "net income", "did we make money", "net profit", "earnings"],
    "GET_DAILY_GOAL": ["daily goal", "target", "sales goal", "revenue target", "did we hit the goal"],
    
    # 📦 INVENTORY
    "CHECK_STOCK": ["stock", "inventory", "low stock", "running out", "shortage", "supplies", "stock alerts", "inventory master"],
    "GET_PRODUCT_COUNT": ["how many products", "product count", "total items", "inventory size", "number of products", "buy more products"],
    "GET_CATEGORIES": ["categories", "category", "product types", "departments", "what do we sell"],
    
    # 🧾 ORDERS
    "GET_TOTAL_ORDERS": ["total orders", "how many orders", "order count", "sales volume", "number of receipts", "transaction count"],
    "GET_RECENT_TRANSACTIONS": ["live transactions", "recent sales", "latest orders", "history", "just sold", "show me sales"],
    
    # 📨 ADMIN
    "GET_FEEDBACK": ["admin inbox", "feedback", "messages", "inbox", "complaints", "suggestions"],
    
    # 🔮 PEOPLE & AI
    "BEST_SELLER": ["best seller", "top product", "most popular", "hot items", "what sells the most"],
    "PREDICT_SALES": ["forecast", "predict", "tomorrow", "future sales", "ai prediction"],
    "GET_STAFF": ["top staff", "best cashier", "who is working", "employee performance", "cashier ranking", "how many staff", "staff count", "number of employees"], 
    "GET_VIP": ["vip", "loyal customer", "top client", "best customer"],
    
    # 🆘 UI HELP
    "GET_UI_HELP": ["export", "import", "download", "theme", "dark mode", "night mode", "light mode", "date", "calendar", "where is", "how do i", "wifi"],
    
    # 👋 CHIT CHAT
    "GREETING": ["hello", "hi", "hey", "help", "good morning"]
}

def clean_text(text):
    # Remove punctuation (like ?) and extra spaces
    text = re.sub(r'[^\w\s]', '', text)
    return " ".join(text.split()).lower()

def classify_intent(raw_text):
    text = clean_text(raw_text)
    best_intent = "UNKNOWN"
    highest_score = 0.0

    for intent, phrases in INTENTS.items():
        for phrase in phrases:
            # Check overlap
            similarity = difflib.SequenceMatcher(None, text, phrase).ratio()
            
            # 🚀 BIG BOOST: If the key phrase is inside the sentence
            if phrase in text: 
                similarity += 0.5 
            
            if similarity > highest_score:
                highest_score = similarity
                best_intent = intent

    # 🎛️ TWEAK: Lowered threshold to 0.5 to catch "short" queries
    if highest_score < 0.5:
        return "UNKNOWN"
        
    return best_intent

@app.route('/classify', methods=['POST'])
def nlu_engine():
    try:
        data = request.json
        user_query = data.get('query', '')
        intent = classify_intent(user_query)
        print(f"🧠 NLP Analysis: '{user_query}' -> {intent}")
        return jsonify({"intent": intent})
    except Exception as e:
        print(f"🔥 NLP Error: {e}")
        return jsonify({"intent": "UNKNOWN"})

# ==========================================
# 🔮 2. THE PREDICTION ENGINE (Regression)
# ==========================================
@app.route('/predict', methods=['POST'])
def predict():
    data = request.json
    if isinstance(data, dict): data = [data]
    
    # Mock data generation if empty (Robustness)
    if not data or len(data) < 2:
        base_qty = 10
        data = [{'date': (datetime.now() - timedelta(days=i)).strftime("%Y-%m-%d"), 'qty': max(1, base_qty + random.randint(-5,5))} for i in range(5,0,-1)]

    processed_data = []
    for d in data:
        try:
            d_str = d['date'] if isinstance(d['date'], str) else d['date'].strftime("%Y-%m-%d")
            dt = datetime.strptime(d_str, "%Y-%m-%d")
            processed_data.append({'date': dt, 'qty': d['qty']})
        except: pass
            
    if not processed_data: return jsonify({"prediction": 0})

    # Linear Regression Math (Standard Library - No Numpy needed)
    start_date = min(d['date'] for d in processed_data)
    X = [(d['date'] - start_date).days for d in processed_data]
    Y = [d['qty'] for d in processed_data]
    
    n = len(X)
    sum_x = sum(X)
    sum_y = sum(Y)
    sum_xy = sum(x*y for x,y in zip(X,Y))
    sum_x2 = sum(x**2 for x in X)
    
    denominator = (n * sum_x2 - sum_x ** 2)
    if denominator == 0: return jsonify({"prediction": int(sum(Y)/n)})

    slope = (n * sum_xy - sum_x * sum_y) / denominator
    intercept = (sum_y - slope * sum_x) / n

    future_day = max(X) + 1
    prediction = int((slope * future_day) + intercept)
    
    return jsonify({"prediction": max(0, prediction), "status": "success"})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)