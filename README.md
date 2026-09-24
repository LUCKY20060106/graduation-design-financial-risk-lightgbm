# Graduation Design: Financial Risk Prediction System for Listed Companies Based on Improved LightGBM

## 📌 Project Overview
This project is a graduation design for Computer Science and Technology. It aims to build a high-precision, interpretable intelligent financial early warning platform for Chinese A-share listed companies (predicting ST risk).

### Key Highlights
- **Improved LightGBM**: Utilizes GBDT framework with leaf-wise growth strategy.
- **SMOTE**: Handles extreme class imbalance (ST companies only account for 2.82% of the dataset).
- **SHAP Interpretation**: Provides marginal contribution analysis for each financial indicator, solving the "black box" problem of AI models.
- **Benchmarking Analysis**: Interactive radar charts comparing company performance against industry averages.
- **Batch Processing**: Supports Excel file uploads for large-scale risk screening.

---

## 🛠 Tech Stack

### Backend
- **Framework**: Spring Boot 3
- **Data Access**: Spring Data JPA
- **Reporting**: Apache POI (Excel)
- **Communication**: ProcessBuilder (StdIn/StdOut communication with Python engine)

### Frontend
- **Framework**: Vue 3 (Composition API)
- **UI Components**: Element Plus
- **Visualization**: ECharts (Radar, Bar, ROC, Confusion Matrix)
- **HTTP Client**: Axios

### AI Engine (Python)
- **Language**: Python 3.12
- **ML Libraries**: LightGBM, Scikit-learn, Imbalanced-learn (SMOTE)
- **Interpretation**: SHAP
- **Data Processing**: Pandas, Numpy

### Infrastructure
- **Database**: MySQL 8.0 (Docker-based)
- **Version Control**: Git

---

## 📂 Project Structure
```text
.
├── backend/            # Spring Boot 3 Backend Project
├── frontend/           # Vue 3 Frontend Project
├── sql/                # Database initialization and migration scripts
├── src/                # Python AI Engine and Data Processing scripts
├── start_all.ps1      # One-click startup script
├── merge_build.ps1    # Front-end and Back-end integration script
├── 项目介绍文档.md      # Project introduction (Chinese)
├── 进度.md             # Project progress log (Chinese)
└── README.md           # This file
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+
- Python 3.12+
- Docker (for MySQL)
- Maven

### Installation & Running

#### 1. Database Setup
```bash
# Run MySQL in Docker
docker run -d --name bishe-mysql -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=risk_db mysql:8.0

# Initialize schema
# Run scripts in ./sql/schema.sql
```

#### 2. All-in-One Merge (Optional but recommended for deployment)
```bash
# This will build frontend and move files to backend static folder
.\merge_build.ps1
```

#### 3. Backend & Database Startup
```bash
# This will start MySQL container and Spring Boot
.\start_all.ps1
```

#### 4. Frontend (Only for independent development)
```bash
cd frontend
npm install
npm run dev
```

#### 4. Python Environment
```bash
python -m venv venv
source venv/bin/activate  # venv\Scripts\activate on Windows
pip install -r requirements.txt
```

---

## 📊 Core Algorithm Logic

### 1. Feature Engineering
- **Missing Value Imputation**: Median filling based on industry benchmarks.
- **Feature Selection**: Variance thresholding to remove redundant indicators.

### 2. SMOTE (Synthetic Minority Over-sampling Technique)
Addresses class imbalance by generating synthetic ST samples in the feature space.
Formula: `x_new = x_i + rand(0,1) * (x_zi - x_i)`

### 3. LightGBM + SHAP
Quantifies marginal contribution of each indicator to the risk score.
SHAP Formula: `φ_i = Σ [ |S|!(n-|S|-1)! / n! ] * [ f(S ∪ {i}) - f(S) ]`

---

## 📜 License
This project is for educational purposes (Graduation Design).

---
*Created by LUCKY20060106*
