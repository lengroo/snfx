# -*- coding: utf-8 -*-
import re, glob, json, os
from collections import Counter
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'tools/hot/*.md')
BAD = ('按场景','挑选','建议','适合','比如','以下','小编','总结','合集','列表','点击','搜索','下载','关注','点赞','收藏','评论','转发','公众号','本文','原文','作者','编辑','标签','阅读','扫码','登录','注册','首页','热搜','图片','视频','外套','毛衣','天气','萤火虫','西瓜','海滩','星空')
SOFT = ('桃花','春雨','酒窝','幸福','甜蜜','婚礼','宝宝','祝福','白头','一生一世','爱恋','痴心','公主','王子','萤火虫','夏日','晚霞')
KEY = ('离开','走了','删','不回','消息','眼泪','遗憾','分手','晚安','错过','沉默','孤独','深夜','放下','舍不得','名字','以后','打扰','敷衍','冷漠','卑微','在乎','想你','心疼','心酸','失恋','喜欢','爱','等')
PUNCT = '、.,．)：:]'
def clean(s):
    s = s.strip()
    for ch in ('**','##','#','>','- ','* '):
        s = s.replace(ch,'')
    out = []
    for ch in s:
        out.append(' ' if ch in '<>[]()' else ch)
    s = ''.join(out)
    while 'http' in s:
        i = s.find('http'); j = s.find(' ', i)
        s = s[:i] + ('' if j < 0 else s[j:])
    return s.strip().strip(' 　')
def tidy(q):
    q = q.strip()
    for sep in ('——','—','|'):
        if sep in q:
            q = q.split(sep)[0]
    keep = []
    for ch in q:
        o = ord(ch)
        if (0x4E00 <= o <= 0x9FA5) or (0x30 <= o <= 0x39) or (0x41 <= o <= 0x5A) or (0x61 <= o <= 0x7A):
            keep.append(ch)
        elif ch in '，。！？、；：…—“”‘’()':
            keep.append(ch)
        elif ch == ' ':
            keep.append(ch)
    q = ''.join(keep).strip()
    q = q.lstrip('0123456789、.．) ').strip()
    q = q.replace('  ', ' ')
    return q
def sentences(s):
    buf = ''; out = []
    for ch in s:
        buf += ch
        if ch in ('。','！','？','!','?','…'):
            out.append(buf); buf = ''
    if buf.strip(): out.append(buf)
    return out
def norm(s):
    return re.sub('[^0-9A-Za-z一-龥]', '', s)
rows = []
for f in sorted(glob.glob(SRC)):
    name = f.split('/')[-1].replace('.md','')
    for ln in open(f, encoding='utf-8', errors='ignore').read().split('\n'):
        c = tidy(clean(ln))
        if len(c) < 12: continue
        segs = sentences(c)
        pool = segs if (len(c) > 80 and len(segs) > 1) else [c]
        for q in pool:
            q = tidy(q)
            if len(q) < 14 or len(q) > 110: continue
            if chr(0xFF1A) in q: continue
            if q[:2] in ('一、', '二、', '三、', '四、', '五、', '六、', '七、'): continue
            if any(b in q for b in BAD): continue
            if any(b in q for b in SOFT): continue
            if q.count('，') > 6: continue
            if '你' not in q and '我' not in q: continue

            sc = 0
            if '你' in q and '我' in q: sc += 4
            sc += sum(1 for w in KEY if w in q)
            if len(q) > 85: continue
            if sc < 1: continue
            rows.append([q, name, sc])
rows.sort(key=lambda r: -r[2])
seen = {}; picked = []
for q, name, sc in rows:
    k = norm(q)
    if len(k) < 9 or k in seen: continue
    seen[k] = 1
    q = q.rstrip('。').strip()
    if not q.endswith(('。','！','？','!','?','…')):
        q = q + '。'
    picked.append([q, name, sc])
    if len(picked) >= 365: break
data = ('\n@@@\n').join([r[0] for r in picked])
open(os.path.join(ROOT, 'app/src/main/assets/hot.txt'), 'w', encoding='utf-8').write(data)
json.dump([{'t': r[0], 'src': r[1]} for r in picked], open(os.path.join(ROOT, 'tools/hot_final.json'), 'w'), ensure_ascii=False)
print('picked', len(picked), 'cands', len(rows))
print(Counter([r[1] for r in picked]).most_common(6))