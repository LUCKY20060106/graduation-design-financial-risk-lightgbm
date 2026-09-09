import docx
import os

def read_docx(file_path):
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return
    
    doc = docx.Document(file_path)
    full_text = []
    for para in doc.paragraphs:
        full_text.append(para.text)
    return '\n'.join(full_text)

file_path = r'd:\桌面\计算机科学与技术毕业论文\Choice智能金融终端访问方式(1).docx'
text = read_docx(file_path)
if text:
    with open('choice_access.txt', 'w', encoding='utf-8') as f:
        f.write(text)
    print("Success")
