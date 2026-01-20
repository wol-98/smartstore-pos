from flask import Flask, request, jsonify
from datetime import datetime
import os

app = Flask(__name__)

# =========================
# 1. THE BRAIN (Prediction Logic)
# =========================
@app.route('/predict', methods=['POST'])
def predict():
    print("\n🔵 Received Prediction Request...")
    data = request.json
    
    # A. Check if data exists
    if not data or len(data) < 2:
        return jsonify({"prediction": 0, "status": "insufficient_data"})

    try:
        # B. Parse Data
        parsed_data = []
        for d in data:
            dt = datetime.strptime(d['date'], "%Y-%m-%d")
            parsed_data.append({'date': dt, 'qty': d['qty']})
        
        # C. Do the Math (Simple Linear Regression)
        # We start counting days from the first sale (Day 0, Day 1...)
        start_date = min(d['date'] for d in parsed_data)
        
        X = [(d['date'] - start_date).days for d in parsed_data]
        Y = [d['qty'] for d in parsed_data]
        
        n = len(X)
        
        sum_x = sum(X)
        sum_y = sum(Y)
        sum_xy = sum(x * y for x, y in zip(X, Y))
        sum_x2 = sum(x ** 2 for x in X)

        denominator = (n * sum_x2 - sum_x ** 2)

        if denominator == 0:
            return jsonify({"prediction": 0, "status": "error_vertical_line"})

        slope = (n * sum_xy - sum_x * sum_y) / denominator
        intercept = (sum_y - slope * sum_x) / n

        # D. Predict Next 7 Days
        last_day = max(X)
        predicted_total = 0
        
        for i in range(1, 8):
            future_day = last_day + i
            prediction = (slope * future_day) + intercept
            predicted_total += max(0, prediction) # Don't predict negative sales

        print(f"🔮 Prediction: {int(predicted_total)} Units")
        return jsonify({"prediction": int(predicted_total), "status": "success"})

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"prediction": 0, "status": "error"})

# =========================
# 2. HEALTH CHECK (Optional)
# =========================
@app.route("/api/test")
def api_test():
    return jsonify({"status": "API Working"})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)