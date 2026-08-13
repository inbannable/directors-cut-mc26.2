package dev.directorscut.event;

import java.util.List;
import java.util.Map;

/** Player-facing clues deliberately describe symptoms, never internal event names. */
public final class HintCatalog {
    private static final Map<String, List<String>> HINTS = Map.ofEntries(
            entry("dont_go_north", "某个方向似乎被刻意提了出来。", "那块牌子不像是写给路过者的。"),
            entry("distant_explosion", "远处的回声来自视线之外……", "空气里残留着一声不太自然的闷响。"),
            entry("someone_was_here", "附近的岩层看起来不像完全天然形成。", "石头后面，似乎曾有人工作过。"),
            entry("the_door", "附近多出了一道本不该存在的轮廓。", "有些入口并不需要通向任何地方。"),
            entry("the_observer", "刚才远处好像有什么东西在看着这里。", "地平线上少了一个你不记得见过的身影。"),
            entry("last_torch", "黑暗里似乎还有一点不属于你的光。", "前方的微光也许不是自然生成的。"),
            entry("sheep_meeting", "附近的羊群安静得有些过分。", "夜色中传来一圈整齐的羊叫。"),
            entry("gerald", "地下传来一声很不合时宜的牛叫。", "附近好像有一头迷路得离谱的牛。"),
            entry("false_boss", "那阵气势的源头……似乎没有想象中高大。", "可怕的声音之后，只剩下一声鸡叫。"),
            entry("wrong_chest", "附近有个箱子，里面的选择似乎异常坚定。", "某处藏着一批用途高度一致的工具。"),
            entry("revenge_of_chickens", "背后的脚步很轻，而且数量不少。", "你杀过的鸡或许有人记得。"),
            entry("very_small_zombie_siege", "围城的声势与兵力似乎不太匹配。", "门外的威胁听起来比实际规模大得多。"),
            entry("the_camp", "风里混着一丝营火与旧煤烟的味道。", "附近也许有人比你更早停留过。"),
            entry("the_tunnel", "附近的石壁后藏着过于笔直的空洞。", "你的矿道似乎快要碰上一条旧路。"),
            entry("perfect_view", "零星的光似乎正指向更高的地方。", "地形在某个方向上格外引人注意。"),
            entry("north_followup", "你走过的方向似乎给出了回应。", "那个警告并没有被完全忘记。"),
            entry("mercy", "最糟的时候，附近似乎少了一点敌意。", "这份好运来得恰到好处，但并不完整。"),
            entry("convenient_chest", "附近可能有一小批不够完美、但足够救急的补给。", "你缺少的东西似乎出现了一部分。"),
            entry("homecoming", "家附近有些细节和离开时不一样。", "这次回家，好像有人替这里等过你。"),
            entry("helpful_skeleton", "刚才有一支箭并不是冲你来的。", "敌人之间短暂地产生了分歧。"),
            entry("storm_timing", "天气像是在等待这一刻。", "天空刚好替眼前这一幕加重了语气。"),
            entry("bad_feeling", "周围的声音忽然显得太空了。", "某种不安先于危险一步到来。")
    );

    private HintCatalog() {
    }

    private static Map.Entry<String, List<String>> entry(String id, String... hints) {
        return Map.entry(id, List.of(hints));
    }

    public static boolean hasHint(String eventId) {
        return HINTS.containsKey(eventId);
    }

    public static String hintFor(String eventId, long variation) {
        List<String> hints = HINTS.get(eventId);
        if (hints == null || hints.isEmpty()) return "附近似乎发生了一点不寻常的变化。";
        return hints.get(Math.floorMod(variation, hints.size()));
    }
}
