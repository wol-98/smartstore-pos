from flask import Flask, request, jsonify
from datetime import datetime
import os

app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    print("\n🔵 Received Prediction Request...")
    data = request.json
    
    # 1. Validation
    if not data or len(data) < 2:
        return jsonify({"prediction": 0, "status": "insufficient_data"})

    try:
        # 2. Prepare Data (X = days passed, Y = quantity)
        # Parse dates and sort just in case
        parsed_data = []
        for d in data:
            dt = datetime.strptime(d['date'], "%Y-%m-%d")
            parsed_data.append({'date': dt, 'qty': d['qty']})
        
        # Find start date to normalize X (Day 0, Day 1, etc.)
        start_date = min(d['date'] for d in parsed_data)
        
        X = [(d['date'] - start_date).days for d in parsed_data]
        Y = [d['qty'] for d in parsed_data]
        
        n = len(X)

        # 3. Calculate Slope (m) and Intercept (b) using algebraic formulas
        # Formula: m = (n*Sum(xy) - Sum(x)*Sum(y)) / (n*Sum(x^2) - (Sum(x))^2)
        sum_x = sum(X)
        sum_y = sum(Y)
        sum_xy = sum(x * y for x, y in zip(X, Y))
        sum_x2 = sum(x ** 2 for x in X)

        denominator = (n * sum_x2 - sum_x ** 2)

        if denominator == 0:
            return jsonify({"prediction": 0, "status": "error_vertical_line"})

        slope = (n * sum_xy - sum_x * sum_y) / denominator
        intercept = (sum_y - slope * sum_x) / n

        # 4. Predict Future (Next 7 days)
        last_day = max(X)
        predicted_total = 0
        
        for i in range(1, 8):
            future_day = last_day + i
            # y = mx + b
            prediction = (slope * future_day) + intercept
            # Prevent negative predictions (cant sell -5 items)
            predicted_total += max(0, prediction)

        print(f"🔮 Prediction: {int(predicted_total)} Units")
        return jsonify({"prediction": int(predicted_total), "status": "success"})

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"prediction": 0, "status": "error"})

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)