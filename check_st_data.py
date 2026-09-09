import pandas as pd

file_path = r'd:\桌面\计算机科学与技术毕业论文\RESSET 股票_股票特别处理与撤销\RESSET_STKSPCTRMT_1.xlsx'
try:
    # Read only first 5 rows to check structure
    df = pd.read_excel(file_path, nrows=5)
    print("Columns:", df.columns.tolist())
    print("\nFirst 5 rows:\n", df)
except Exception as e:
    print(f"Error reading Excel: {e}")
