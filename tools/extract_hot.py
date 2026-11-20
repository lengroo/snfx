# -*- coding: utf-8 -*-
import re, glob, json, random, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
POS = ['你','我','爱','心','离开','等','忘','遗憾','错过','眼泪','喜欢','分手','孤独','消息','回复','失眠','深夜','时间','我们','走','回头','以后','难过','沉默','想念','放不下','一个人','舍不得','敷衍','冷漠','消失','删除','聊天框','承诺','未来','认真','在乎','卑微','将就','放下','名字','晚安']
BAD = ('选择项目','选择模型','下载','登录','注册','热搜榜','京ICP','广告','扫码','版权','相关推荐','猜你喜欢','返回首页','栏目','访问','评论','点赞','收藏','分享','阅读全文','展开','导航')
PUNCT = chr(0x3001) + '.,' + chr(0xFF0E) + ')' + chr(0xFF09) + chr(0xFF1A) + ':]'
def clean(s):
    s = s.strip()
    for ch in ('**','##','#','>','- ','* '):
        s = s.replace(ch,'')
    while '](' in s and '[' in s:
        i = s.find('['); j = s.find('](')
        if i < 0 or j < 0 or i > j: break
        k = s.find(')', j)
        if k < 0: break
        s = s[:i] + s[j+2:k] + s[k+1:]
    s = s.replace('<','').replace('>','')
    return s.strip().strip(' ' + chr(0x3000) + chr(0x22) + chr(0x60))
def strip_num(s):
    m = 0
    while m < len(s) and (s[m].isdigit() or s[m] in PUNCT):
        m += 1
    return s[m:].strip()
def split_sent(s):
    buf = ''; out = []
    for ch in s:
        buf += ch
        if ch in (chr(0x3002), chr(0xFF01), chr(0xFF1F), '!', '?', chr(0x2026)):
            out.append(buf); buf = ''
    if buf.strip(): out.append(buf)
    return out
def norm(s):
    return re.sub('[^0-9A-Za-z' + chr(0x4E00) + '-' + chr(0x9FA5) + ']', '', s)
cands = []
for f in sorted(glob.glob(os.path.join(ROOT, 'tools/hot/*.md'))):
    for ln in open(f, encoding='utf-8', errors='ignore').read().split(chr(10)):
        c = strip_num(clean(ln))
        if len(c) < 10: continue
        segs = split_sent(c)
        pool = segs if (len(c) > 90 and len(segs) > 1) else [c]
        for q in pool:
            q = q.strip()
            if len(q) < 12 or len(q) > 150: continue
            if not re.search('[' + chr(0x4E00) + '-' + chr(0x9FA5) + ']', q): continue
            if any(b in q for b in BAD): continue
            if q.count(chr(0xFF0C)) > 6: continue
            cands.append((f.split('/')[-1], q))
seen = {}; order = []
for f, q in cands:
    k = norm(q)
    if len(k) < 9: continue
    if k in seen: continue
    sc = sum(1 for w in POS if w in q)
    seen[k] = [q, f, sc]
    order.append(seen[k])
order = [p for p in order if len(p[0]) >= 12]
print('cands', len(cands), 'unique', len(order))
order.sort(key=lambda x: (-x[2], -len(x[0])))
print('--- top 12 ---')
for p in order[:12]: print(p[2], len(p[0]), p[0][:70])
random.seed(3)
print('--- sample ---')
for p in random.sample(order, min(12, len(order))): print(len(p[0]), p[0][:80])
json.dump([p[0] for p in order], open(os.path.join(ROOT, 'tools/hot_pool.json'), 'w'), ensure_ascii=False)