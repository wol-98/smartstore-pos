from flask import Flask, request, jsonify
import numpy as np
from datetime import datetime
import os

app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    print("\n🔵 Received Prediction Request...")
    data = request.json
    
    if not data or len(data) < 2:
        return jsonify({"prediction": 0, "status": "insufficient_data"})

    # 1. PREPARE VECTORS
    dates = [datetime.strptime(d['date'], "%Y-%m-%d") for d in data]
    start_date = min(dates)
    
    X_vals = [(d - start_date).days for d in dates]
    X = np.array([[x, 1] for x in X_vals]) 
    y = np.array([d['qty'] for d in data])

    try:
        # 2. COMPUTE COEFFICIENTS (Normal Equation)
        X_T = X.T
        beta = np.linalg.inv(X_T @ X) @ X_T @ y
        slope, intercept = beta[0], beta[1]
        
        # 3. PREDICT FUTURE (Next 7 Days)
        last_day = max(X_vals)
        future_days = [last_day + i for i in range(1, 8)]
        predicted_total = sum([max(0, (slope * day) + intercept) for day in future_days])

        print(f"🔮 Prediction: {int(predicted_total)} Units")
        return jsonify({"prediction": int(predicted_total), "status": "success"})

    except np.linalg.LinAlgError:
        return jsonify({"prediction": 0, "status": "singular_matrix_error"})

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)