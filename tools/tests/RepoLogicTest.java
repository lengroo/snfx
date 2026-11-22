import com.lengroo.snfx.QuoteRepo;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 纯逻辑测试：不依赖 Android 运行时，直接 java 跑。 */
public class RepoLogicTest {

    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("  ok   " + name);
        } else {
            fail++;
            System.out.println("  FAIL " + name);
        }
    }

    private static String key(Calendar c) {
        return String.format("%04d%02d%02d", c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static void main(String[] args) {
        System.out.println("QuoteRepo 纯逻辑测试");

        Set<Integer> s = QuoteRepo.parseSeen("1,3,7");
        check("parseSeen 正常三个", s.size() == 3);
        check("parseSeen 含非法项只留合法", QuoteRepo.parseSeen("1,x,3").size() == 2);
        check("parseSeen 空串得空集", QuoteRepo.parseSeen("").isEmpty());
        check("joinSeen 往返", QuoteRepo.joinSeen(QuoteRepo.parseSeen("4,5,6")).equals("4,5,6"));

        Map<String, Integer> d = QuoteRepo.parseDaily("20260101:3;20260102:0;bad");
        check("parseDaily 合法两条", d.size() == 2);
        check("parseDaily 取值为 3", d.get("20260101") == 3);
        check("joinDaily 往返", QuoteRepo.joinDaily(d).contains("20260101:3"));

        Calendar base = Calendar.getInstance();
        base.set(2026, Calendar.NOVEMBER, 23, 12, 0, 0);

        LinkedHashMap<String, Integer> three = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < 3; i++) {
            Calendar x = (Calendar) base.clone();
            x.add(Calendar.DAY_OF_YEAR, -i);
            three.put(key(x), 2);
        }
        check("连续三天 = 3", QuoteRepo.streakFrom(three, 2026, 11, 23) == 3);

        LinkedHashMap<String, Integer> skipToday = new LinkedHashMap<String, Integer>();
        for (int i = 1; i <= 2; i++) {
            Calendar x = (Calendar) base.clone();
            x.add(Calendar.DAY_OF_YEAR, -i);
            skipToday.put(key(x), 1);
        }
        check("今天没看，从昨天起算 = 2", QuoteRepo.streakFrom(skipToday, 2026, 11, 23) == 2);

        LinkedHashMap<String, Integer> gap = new LinkedHashMap<String, Integer>();
        Calendar t0 = (Calendar) base.clone();
        gap.put(key(t0), 1);
        Calendar t2 = (Calendar) base.clone();
        t2.add(Calendar.DAY_OF_YEAR, -2);
        gap.put(key(t2), 5);
        check("中间断一天 = 1", QuoteRepo.streakFrom(gap, 2026, 11, 23) == 1);

        check("空记录 = 0", QuoteRepo.streakFrom(new LinkedHashMap<String, Integer>(), 2026, 11, 23) == 0);

        System.out.println("通过 " + pass + " / 失败 " + fail);
        if (fail > 0) System.exit(1);
    }
}
