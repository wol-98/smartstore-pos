from flask import Flask, jsonify

app = Flask(__name__)

# =========================
# FRONTEND ROUTE (Simple Text for now)
# =========================
@app.route("/")
def dashboard():
    return "🧠 AI Brain is Online!" 

# =========================
# TEST API (Health Check)
# =========================
@app.route("/api/test")
def api_test():
    return jsonify({"status": "API Working"})

if __name__ == "__main__":
    # The port is handled by Railway automatically, but 5000 is a good default
    app.run(host='0.0.0.0', port=5000)