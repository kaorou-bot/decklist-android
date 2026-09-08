# 法术力符号资源

2026-09-08 从 Scryfall Symbology API https://api.scryfall.com/symbology 获取 84 个符号 SVG，转换成 Android VectorDrawable，保持路径、颜色和比例。资源路径为 app/src/main/res/drawable/mana_*.xml。无需运行时联网或新增渲染依赖。

图形来源：https://svgs.scryfall.io/card-symbols/ （每个文件与 drawable 去除 mana_ 前缀后的名称对应，原始文件名大写）。万智牌符号图形属于 Wizards of the Coast；Scryfall 是分发来源，并非本应用或图形的官方授权声明。

保留原始费用字符串，仅显示层替换花括号内符号；未知符号原样保留。支持通用数字、W/U/B/R/G、C、S、X/Y/Z、混色、单色混色、非瑞克西亚及规则文本中的横置等符号。数字和 X 仍显示数字和 X，但使用牌面图案，不转换成其他图形。
