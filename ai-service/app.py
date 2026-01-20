from flask import Flask, request, jsonify
from datetime import datetime, timedelta
import os
import random

app = Flask(__name__)

# =================================================
# HELPER: SIMULATE HISTORY (The "Hallucination" Engine)
# =================================================
def generate_mock_data(real_data):
    """
    If we don't have enough data, we create 'fake' past sales 
    based on the current sale to show the user how the AI works.
    """
    # Take the last real sale as a baseline
    base_qty = real_data[0]['qty'] if real_data else 10
    base_date = datetime.now()
    
    mock_data = []
    # Create 5 days of fake history
    for i in range(5, 0, -1):
        past_date = base_date - timedelta(days=i)
        # Randomize sales slightly (e.g., +/- 5 units)
        random_qty = max(1, base_qty + random.randint(-5, 5))
        mock_data.append({
            'date': past_date, 
            'qty': random_qty
        })
    
    return mock_data

# =========================
# THE BRAIN (Prediction Logic)
# =========================
@app.route('/predict', methods=['POST'])
def predict():
    print("\n🔵 Received Prediction Request...")
    data = request.json
    
    # === UPGRADE: USE MOCK DATA IF HISTORY IS SHORT ===
    if not data or len(data) < 2:
        print("⚠️ Not enough real data. Activating SIMULATION MODE.")
        # If we have at least 1 real point, use it as seed; otherwise default
        processed_data = generate_mock_data(data)
    else:
        # Use real data if we have it
        processed_data = []
        for d in data:
            dt = datetime.strptime(d['date'], "%Y-%m-%d")
            processed_data.append({'date': dt, 'qty': d['qty']})
            
    # ==================================================

    try:
        # 1. Prepare X (Days) and Y (Qty)
        start_date = min(d['date'] for d in processed_data)
        X = [(d['date'] - start_date).days for d in processed_data]
        Y = [d['qty'] for d in processed_data]
        
        n = len(X)
        
        # 2. Linear Regression Math (No Numpy needed)
        sum_x = sum(X)
        sum_y = sum(Y)
        sum_xy = sum(x * y for x, y in zip(X, Y))
        sum_x2 = sum(x ** 2 for x in X)

        denominator = (n * sum_x2 - sum_x ** 2)

        if denominator == 0:
            return jsonify({"prediction": 0, "status": "error_vertical_line"})

        slope = (n * sum_xy - sum_x * sum_y) / denominator
        intercept = (sum_y - slope * sum_x) / n

        # 3. Predict Next 7 Days
        last_day = max(X)
        predicted_total = 0
        
        # Predict Day 1 to Day 7 into the future
        for i in range(1, 8):
            future_day = last_day + i
            prediction = (slope * future_day) + intercept
            predicted_total += max(0, prediction)

        print(f"🔮 Prediction (Simulated): {int(predicted_total)} Units")
        return jsonify({
            "prediction": int(predicted_total), 
            "status": "success_simulated" if len(data) < 2 else "success_real"
        })

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"prediction": 0, "status": "error"})

@app.route("/api/test")
def api_test():
    return jsonify({"status": "AI Brain Online"})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)