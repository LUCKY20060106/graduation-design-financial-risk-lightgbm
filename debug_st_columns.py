import pandas as pd
import os

file_path = r'd:\桌面\计算机科学与技术毕业论文\RESSET 股票_股票特别处理与撤销\RESSET_STKSPCTRMT_1.xlsx'
df = pd.read_excel(file_path, nrows=5)
print("实际列名:", df.columns.tolist())
