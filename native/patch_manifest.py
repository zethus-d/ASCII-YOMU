p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
f = open('native/intent-filters.xml', encoding='utf-8').read()
assert '</activity>' in s
s = s.replace('</activity>', f + '\n</activity>', 1)
open(p, 'w', encoding='utf-8').write(s)
print('manifest patched')
