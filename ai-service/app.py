from flask import Flask, request, jsonify
from datetime import datetime, timedelta
import os
import random

app = Flask(__name__)

# =================================================
# HELPER: SIMULATE HISTORY
# =================================================
def generate_mock_data(real_data):
    # Safe extraction of the first item
    if real_data and isinstance(real_data, list):
        base_qty = real_data[0].get('qty', 10)
    else:
        base_qty = 10
        
    base_date = datetime.now()
    mock_data = []
    
    for i in range(5, 0, -1):
        past_date = base_date - timedelta(days=i)
        random_qty = max(1, base_qty + random.randint(-5, 5))
        mock_data.append({'date': past_date, 'qty': random_qty})
    
    return mock_data

# =========================
# THE BRAIN
# =========================
@app.route('/predict', methods=['POST'])
def predict():
    print("\n🔵 Received Prediction Request...")
    data = request.json
    
    # === BUG FIX: Handle Single Object vs List ===
    if isinstance(data, dict):
        print("⚠️ Received single object, converting to list.")
        data = [data]
        
    # === UPGRADE: USE MOCK DATA IF HISTORY IS SHORT ===
    if not data or len(data) < 2:
        print("⚠️ Not enough real data. Activating SIMULATION MODE.")
        processed_data = generate_mock_data(data)
    else:
        processed_data = []
        for d in data:
            try:
                dt = datetime.strptime(d['date'], "%Y-%m-%d")
                processed_data.append({'date': dt, 'qty': d['qty']})
            except Exception as e:
                print(f"Skipping bad data point: {d}")
            
    try:
        if not processed_data:
            return jsonify({"prediction": 0, "status": "no_data"})

        # 1. Prepare X (Days) and Y (Qty)
        start_date = min(d['date'] for d in processed_data)
        X = [(d['date'] - start_date).days for d in processed_data]
        Y = [d['qty'] for d in processed_data]
        
        n = len(X)
        if n < 2: return jsonify({"prediction": sum(Y), "status": "fallback"})

        # 2. Linear Regression Math
        sum_x = sum(X)
        sum_y = sum(Y)
        sum_xy = sum(x * y for x, y in zip(X, Y))
        sum_x2 = sum(x ** 2 for x in X)

        denominator = (n * sum_x2 - sum_x ** 2)
        if denominator == 0:
            return jsonify({"prediction": int(sum(Y)/n * 7), "status": "average"})

        slope = (n * sum_xy - sum_x * sum_y) / denominator
        intercept = (sum_y - slope * sum_x) / n

        # 3. Predict Next 7 Days
        last_day = max(X)
        predicted_total = 0
        for i in range(1, 8):
            future_day = last_day + i
            prediction = (slope * future_day) + intercept
            predicted_total += max(0, prediction)

        print(f"🔮 Prediction: {int(predicted_total)}")
        return jsonify({
            "prediction": int(predicted_total), 
            "status": "success"
        })

    except Exception as e:
        print(f"🔥 Critical Error: {e}")
        return jsonify({"prediction": 0, "status": "error"})

@app.route("/api/test")
def api_test():
    return jsonify({"status": "AI Brain Online"})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)