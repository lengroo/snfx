import re, glob, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(ROOT, 'app/src/main/assets/quotes.txt')
SEP='\n@@@\n'
def norm(s):
    return re.sub('[^0-9A-Za-z\u4e00-\u9fa5]','',s)
def parse(path):
    out=[]
    for ln in open(path,encoding='utf-8').read().split('\n'):
        m=re.match(r'^\s*\d+\s*[、.．)]\s*(.+)$', ln)
        if m:
            t=m.group(1).strip()
            if len(t)>=8: out.append(t)
    return out
files=sorted(glob.glob(os.path.join(ROOT, 'tools/import/*.txt')))
imported=[]
for f in files: imported += parse(f)
cur=[x.strip() for x in open(DST,encoding='utf-8').read().split(SEP) if x.strip()]
seen=set(); merged=[]
for t in imported+cur:
    k=norm(t)
    if not k or k in seen: continue
    seen.add(k); merged.append(t)
open(DST,'w',encoding='utf-8').write(SEP.join(merged))
print('files',len(files),'imported',len(imported),'merged',len(merged),'added',len(merged)-len(cur))