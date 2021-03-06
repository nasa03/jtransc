package regexodus;

import com.jtransc.annotation.JTranscInvisible;
import com.jtransc.io.JTranscConsole;
import regexodus.ds.CharArrayList;
import regexodus.ds.CharCharMap;
import regexodus.ds.IntBitSet;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Unicode categories constructed from tightly compressed string and array literals instead of large resources.
 * Credit for the technique and much of the code goes to gagern, https://gist.github.com/gagern/89db1179766a702c564d
 * Also, the heavy amount of work that went into the Unicode DB for Node.JS (which the pre-processing stage for this
 * depends on) must be commended; that project is https://github.com/mathiasbynens/node-unicode-data
 */
@JTranscInvisible
public class Category {
    private final int[] d;
    private final String s;
    public final int length;
    private int n;
    private CharArrayList cal;
    final Block[] blocks;
    private Category()
    {
        d = new int[0];
        s = "";
        length = 0;
        blocks = new Block[0];
    }
    private Category(int[] directory, String data)
    {
        d = directory;
        s = data;
        n = s.length();
        int j = 0, len = 0;
        cal = new CharArrayList(n);
        for (int i = 0; i < n; ++i) {
            cal.add(j += d[s.codePointAt(i) - 32]);
            if((i & 1) == 1) len += 1 + j - cal.getChar(i-1);
        }
        length = len;
        blocks = makeBlocks();
    }

    public char[] contents()
    {
        int k = 0;
        char[] con = new char[length];
        for (int i = 0; i < n - 1; i += 2)
            for (char e = cal.getChar(i); e <= cal.getChar(i+1); ++e)
                con[k++] = e;
        return con;
    }

    private Block[] makeBlocks() {
        int k = 0;
        Block[] bls = new Block[256];
        IntBitSet[] bss = new IntBitSet[256];
        int e, e2, eb, e2b;
        for (int i = 0; i < n - 1; i += 2) {
            e = cal.getCodePoint(i);
            e2 = cal.getCodePoint(i+1);
            eb = e >>> 8;
            e2b = e2 >>> 8;
            if(bss[eb] == null) bss[eb] = new IntBitSet();
            if(eb == e2b)
            {
                bss[eb].set(e & 0xff, e2 & 0xff);
                continue;
            }
            bss[eb++].set(e & 0xff, 255);

			while (eb != e2b) {
				if (eb > e2b) {
					JTranscConsole.error("Problem in regexodus.Category.makeBlocks");
					break;
				}
                if(bss[eb] == null) bss[eb] = new IntBitSet();
                bss[eb++].set(0, 255);
            }
            if(bss[e2b] == null) bss[e2b] = new IntBitSet();
            bss[e2b].set(0, e2 & 0xff);
        }
        for (int i = 0; i < 256; i++) {
            if(bss[i] == null)
                bls[i] = new Block();
            else
                bls[i] = new Block(bss[i]);
        }
        return bls;
    }

    public boolean contains(char checking) {
        for (int i = 0; i < n - 1; i += 2) {
            if (checking >= cal.getChar(i) && checking <= cal.getChar(i + 1))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Category{" +
                cal +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;

        if (length != category.length) return false;
        if (n != category.n) return false;
        if (!Arrays.equals(d, category.d)) return false;
        if (s != null ? !s.equals(category.s) : category.s != null) return false;
        if (cal != null ? !cal.equals(category.cal) : category.cal != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(blocks, category.blocks);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(d);
        result = 31 * result + (s != null ? s.hashCode() : 0);
        result = 31 * result + length;
        result = 31 * result + n;
        result = 31 * result + (cal != null ? cal.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(blocks);
        return result;
    }

    public static final Category C=new Category(new int[]{0,1,2,3,4,5,8,6,7,11,9,13,10,16,14,15,23,12,27,24,25,31,32,37,40,17,19,20,34,39,42,48,56,60,21,22,28,29,30,33,36,44,45,46,50,52,54,55,57,63,64,65,87,90,95,104,35,41,43,47,49,58,59,61,62,66,68,70,71,73,77,79,85,89,96,100,102,107,117,125,127,136,141,149,162,175,185,191,192,199,215,224,247,283,349,366,367,378,398,620,670,715,1166,1813,6839,8451,11173,21015}," 5j6. \205!(#& \" B \202 =!& 8 #!$ @(D$'-0!x \\!A!l+A$[!- E!\"RCJt *!#!0 & \"\"%!,!#!%(\"## '!0$$ (##!0 & # # #!\" '##!$\"\"'% \"'9,$ , $ 0 & # '!) $ $!\".%!+'\"'$ *!#!0 & # '!,!#!$(### '!:*# (\"$ %\"# \" #\"#\"$\"+#'\"$ %!\"%\"+C$% * $ 3 9\"* $ %'# $$%!)(* $ * $ 3 ) '!, $ %'#'\" %!) #+$ * $ >!* $ '(\"''!9\"&!# :\"4 , \"!&\"\"#( \" *%)!$)^#FH# \"!# \"!\"%% & $ \" \"!# . $!' \" (!)!%5e 7#8 7 - .Hy \"$\"!\201 %!& \" %!> %!< %!& \" %!- ] %!b!G\"2%T!(!\204\"U'. &,3&B). $ #)V!)%)%/!)%i(I$d*6 +#+#\"\"Z!',J#2%1\"Q!a F!1%)%/!-Rf#K\"n(_\"-\"MQ*(8 #%|%}!(!=!(!* \" \" \" 6!N - /!( ;!$ , 1$2$L/#!D .\"6-<.r#\203$831;\207!G!X\"+ *4%/? ? s$K \"$\"!P'#+4&& & & & & & & & kA2 U)z4+#S T!W$>\"V I$7)? { \210*\213Y\206\"@&~:v(u!*`N#)%P(c&+%5!h,5\"g 1#< @&/!)!W0E*(!(!(&& & O*p!)%\212)3#L\211\200!m7&)'$2 ' \" # # o-\177/S!O=/!2%M ; %#' q#w\"(!(!(!$\"& &1#! ");

    public static final Category Co=new Category(new int[]{1792,6399,57344},"\"! ");

    public static final Category Cn=new Category(new int[]{0,1,2,3,4,5,8,6,7,11,9,13,10,16,14,15,23,24,27,12,25,37,40,17,19,20,30,31,32,34,39,42,48,56,21,22,28,29,33,36,44,45,46,52,54,55,57,60,61,63,64,65,87,90,95,102,104,35,41,43,47,50,58,59,62,66,68,70,71,73,77,79,85,89,100,107,117,125,127,136,141,149,162,175,185,191,199,215,224,241,247,283,349,366,378,398,620,670,888,1166,1813,6839,8815,11173,21015},"\202!(#& \" B \177 >!& 6 #!$ A(D$',: y P!W+O$\\!- E!\"RCIr *!#!0 & \"\"%!,!#!%(\"## '!0$$ (##!0 & # # #!\" '##!$\"\"'% \"'7,$ , $ 0 & # '!) $ $!\".%!+'\"'$ *!#!0 & # '!,!#!$(### '!8*# (\"$ %\"# \" #\"#\"$\"+#'\"$ %!\"%\"+C$% * $ 1 7\"* $ %'# $$%!)(* $ * $ 1 ) '!, $ %'#'\" %!) #+$ * $ ?!* $ '(\"''!7\"&!# 8\"4 , \"!&\"\"#( \" *%)!$)_#:G# \"!# \"!\"%% & $ \" \"!# . $!' \" (!)!%;e 5#6 5 - .Gv \"$\"!~ %!& \" %!? %!= %!& \" %!- ^ %!b!F\"2%T!(!\201\"U'. &,1&B). $ #)V!)%)%- )%i(H$d*< +#+#\"\"[!',I#2%3\"Q!a :!3%)%/!-Rf#J\"l(P\"-\"KQ*(6 #%z%{!(!>!(!* \" \" \" <!L - /!( 9!$ , W +!D .\"<-=.p#\200$6139\204!F!Y\"+ *4%/@ @ q$J \"$\"!N'#+4&& & & & & & & & jO2 U)w4+#S T!X$?\"V H$5)@ x \205*\210Z\203\"A&|8t(s!*`L#)%N(c&+%;!h,;\"g 3#= A&/!)!X0E*(!(!(&& & M*n!)%\207)1#]#\206!k5&)'$2 ' \" # # m-}/S!M>/!2%K 9 %#' o!\" u\"(!(!(!$\"& &*'! ");

    public static final Category Cc=new Category(new int[]{0,31,32,96}," !#\"");

    public static final Category Cf=new Category(new int[]{0,4,2,50,5,9,23,27,173,193,250,1363,2045,4351,56976},"( +$& ) # - ,!'!#!\"%. *\"!");

    public static final Category Cs=new Category(new int[]{2047,55296},"! ");

    public static final Category L=new Category(new int[]{2,0,3,4,1,6,5,7,17,8,11,15,10,12,22,25,9,21,13,16,30,46,19,37,40,42,14,18,24,29,35,43,53,20,26,27,32,36,23,28,33,34,38,48,49,50,51,52,55,56,59,63,64,65,68,69,85,88,102,117,31,39,41,45,47,54,66,72,73,74,75,80,81,82,83,87,89,93,94,98,105,107,108,114,116,130,132,134,138,165,191,268,277,332,362,365,457,470,513,619,1164,2684,6581,8453,11171,20949},"U/'/K!*!&!%. 4 \200&*+#)! !u# $\"\" !'!   ! 6 i x0y 7\"!)JdB% 59E$ o !3$)$* \"!(! =4Y-!/D,$&!%1&!,!#!<<cAg@#!6!)03+&'\"$\"1 % !#\"#!(!:$  +$A&&$\"1 % $ $ $D\" !A ()   1 % $ ##!6!3$<!-'\"$\"1 % $ ##!\\$  3!;! &#  \"#$ ! $#$# #*F!@'   . +#!C %$E'   . 0 ##!H! $3$6'   8\"!(!( /&%(#F ) !\"%R` $2%R$ !\"$ !\"!'\" %   ! !\"$ \" $,!\"# !.\"H!T' >G#t91!(&&\"#!#$) &-2!;7 !%!\"9 } \"\"% ! \"\"8 \"\"D \"\"% ! \"\": Q \"\"bJ+(X\"&#\203\"3 /%e'')- \"+(+(+-  3NE!&!Vk08 !%W*4M=\"#-?&/P.,Oj!n5;%Q=:$*?C>9 *>r\" \"#$,zU|\"&\"7\"&\"' ! ! ! 4\"O % !#  %#\"\"&&-%  %[!:!(-Z!&!\"0 !##'! ! ! \" ,\"\"%#&!@$\2055 5 v'\"#$27 !%!\"P)!(.,% % % % % % % %h!\201$?#%$&X'  l \"%8#m;Ba+\202\206f\211?\204V_\"{#+*$15(4\"WM)\"Z\"I\"'T,   \" .4N+LS&#! !2C*.BG)5=!(# 0*# 8<  '1.#!#L !#$\"#\"! !/ \",) 2&\"&\"&,% % 9 0*s4\2102.&K\207\177\"p]%2#%! 0 - # ! $ $ qI~6S\"@^*[# w7/'/-Y#&\"&\"&\" >");

    public static final Category Lu=new Category(new int[]{0,2,3,1,4,9,7,5,6,8,10,11,13,12,25,37,50,16,20,22,34,36,46,49,62,65,73,85,102,136,197,263,290,723,2571,2685,2890,22379,31054},"9.<3!(4 ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! \" ! ! ! ! ! ! ! \" ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !#! ! $#! !#!!\"\"!#!!$#!#! ! !#! \" !#!!! !#$ ) \" \" \" ! ! ! ! ! ! ! \" ! ! ! ! ! ! ! ! \" \" !!! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! )#!#\" !\"! ! ! ! @ ! $ % & !!! !#!1!)5 \"!$ ! ! ! ! ! ! ! ! ! ! ! ( \" !#\"07 ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! * ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !#! ! ! ! ! ! \" ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! \"/D/! ( A;B ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! * ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! *&%'+&%&%'- ! ! ! %&:\",\",\",$-\"? ' $!\"!\" $$& ! ! !\"\"\"+#( 8 C60 !!\" ! ! !\"! \" %!! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! % ! ' F ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! 2 ! ! ! ! ! ! ! ! ! ! ! ! ! = ! ! ! ! ! ! $ ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! + ! !#! ! ! ! ' ! \" ! $ ! ! ! ! ! ! ! ! ! !\"\"$! E.>");

    public static final Category Ll=new Category(new int[]{0,2,3,1,4,5,7,9,6,11,8,13,12,25,28,42,10,19,20,23,26,33,34,37,38,43,46,47,50,52,54,59,64,67,68,79,97,103,136,165,194,275,822,1066,2307,2732,3697,20289,30996},"D-? /3!&! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !#! ! ! ! ! ! ! !#! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! \" ! !!\" ! \" $#% \" $!\" \" ! ! \" !#! \" $ ! \"#\"!& \" \" ! ! ! ! ! ! ! !#! ! ! ! ! ! ! ! !#\" ! $ ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !(\" \"#! % ! ! ! !B!4H ! $ $!1 .6!#$!! ! ! ! ! ! ! ! ! ! ! !$! \" \"#=;! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! 0 ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! \" ! ! ! ! ! !#! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! <8N%L9@,!5E ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !*! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !*'%)&'&'%)&'&'+\"&'&'&'$!#& $!!#'\"\"#'&)!!#I $#$ . % % \"#'\"% > M:\" $#! ! ! % !#!%( ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !#* ! % +7! ( P ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! 2 ! ! ! ! ! ! ! ! ! ! ! ! ! F ! ! ! ! ! !!! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! ! !&! ! \" ! ! ! ! % ! \" !!! ! ! ! ! ! ! ! ! ! , ! A J/(%)CO(+$K-G");

    public static final Category Lt=new Category(new int[]{0,7,3,9,13,16,39,48,453,7574},"( \" \" & )!#!#!$ % ' ");

    public static final Category Lm=new Category(new int[]{0,1,5,4,2,6,8,14,17,470,3,10,11,12,15,22,23,24,29,32,35,36,44,46,62,81,94,96,98,104,108,109,112,115,122,128,134,138,165,175,192,231,242,270,271,329,465,479,566,612,688,690,1237,1251,1755,3040,21521,28439},"R(\",.#& $ D % O I F!K!\" 3 + # M T C P V > Q N\"G8' 45S ' (-W!J H ) 6#% <!:$Y U\"L A 2!B&9 1 @!) 0 E ? /!=*X 7!;");

    public static final Category Lo=new Category(new int[]{2,0,3,4,1,6,5,15,7,17,11,8,9,22,12,21,25,16,10,30,40,13,14,19,20,24,29,43,46,18,26,27,32,34,35,42,51,53,68,23,28,31,33,36,41,49,50,54,55,56,63,66,69,114,170,38,39,44,47,48,52,59,60,64,67,72,74,75,80,85,88,89,93,98,104,105,106,107,116,117,134,146,177,209,257,267,331,362,365,513,552,619,828,1087,1142,3064,6581,8453,11171,20949},"V!1!t!&\"s!|>% <I ,K$ i !0$* \"!)! :3f.!0@-/;9a8dE#!7!+,)6&(\"$\"/ % !#\"#!)!6$  '$8&&$\"/ % $ $ $@\" !8 )+   / % $ ##!7!1$9!.(\"$\"/ % $ ##!I$  1!=! &#  \"#$ ! $#$# #*G!E(   - '#!? %$K(   - , ##!J! $1$7(   4\"!)!) 0&%)#G + !\"%]Z $5&^$ !\"$ !\"!(\" %   ! !\"$ \" $2!\"#9\"J!_( BH#nC/!)&&\"#!#$+ &.5!SC\"v \"\"% ! \"\"4 \"\"@ \"\"% ! \"\"6 Q \"\"SW'U{\"1 0%b((+. \"')')'.  1DL!FA D,4 !%T*3N:\"#.;&0P-2\\r<=%Q:6$*;?BC *:U\" \"#$}\"\177P0-2% % % % % % % %z!O!&e,! g&!%4#h=>O'y\200c\203;8 ~FX,u&'*$`!NTV!j!#%   \" -3D'MR&#! !5?*->H+<<#\"+*# 49  (/' &#!#M !#$\"#\"! !0$#2+!'&\"&\"&2% %qA3\2025-&[\201x\"kF! , . # ! $ $ mAw7R\"EL*o# pl, Y\"3#&\"&\"&\" B");

    public static final Category M=new Category(new int[]{2,0,1,3,4,5,6,10,11,31,30,7,12,13,57,8,9,17,19,28,49,59,14,15,16,20,24,26,32,35,44,46,48,51,53,21,33,36,37,39,41,42,45,50,55,58,60,62,65,66,68,71,73,75,81,89,92,97,99,101,102,106,111,119,136,142,153,156,199,220,237,264,273,276,464,555,704,721,738,768,947,3071,20273,30165},"o^i&g> ! \" \" !T'491!\\&#%#\" #E!);X'5/=# /   $> `<L  1 &(\"* .! &#\"# '!(\"* .! $%\"# $!)\"$!, .! +    C\"* .! &#\"# 0\"(\")!N$$  #'!H#5&   #/\",\"* .! &   #/\",\"* 5&   #'!(\")\"S!%% ! +2\"O!#&-+Z!#% \",%U\"3! ! !%\"K2 \"&' ='![2:#%   #&$#6( !(#l p * *\")\"P)'!? c!_(%(e$M0 3#!46Q$@8G/- ),.-42b  9%!% $\"dB+#m<q a!Y)k%]\"s# 0D\"V\"h!$!%!:$W\"A831B+;,J#@-F!R--!0\"? A!  #\"&\" !I$&\"f+ \"r!n717j");

    public static final Category Mn=new Category(new int[]{0,2,1,3,5,4,6,7,9,10,30,12,13,31,59,8,17,21,26,49,11,15,28,33,35,41,44,53,56,58,62,103,14,16,20,36,43,46,47,48,51,52,57,64,65,67,68,73,75,81,91,92,97,99,102,108,111,119,136,142,153,156,158,199,220,239,266,273,276,464,555,704,721,738,768,947,3071,20273,30165},"jXd%b:! !\"!\"! O)3B0 V&#$#\"!#C -2S)./8#!/!!!%:!Z-< ! $'$ %&4\"* . $#( 1\"*\"= $\"$\"#!% -\"% +\"= $%!\"$ 1\"* . # !#( ( +\"- > , H >!&!!#/\"+\"* . # ' &\"1\"* K#( 1\"? /!! R #&,'U #$!\"+$P\"6 ! ! <,!%!\"&)!8) ?#!$!\"#\"2\"$!0#@ #\"' A g!k!*!*\"-\"L\"!&( #)) E!] Y!$\") '!`\"# . !&! ! #''(# 3,M#3 !%! & 9/,\"7##\"!!J !\"% !!.'#\"\\!!+!&$ ' %\"_;'#h+$ %4l![ T-f#W\"n $(7\"Q\"c % $ 2\"^ 60;'2)F!3 ### 9 N$#\"#\", ( G I !!#\"&\"! D\"( a # $ m i505e");

    public static final Category Me=new Category(new int[]{2,0,1,3,1160,1567,5685,34188},"$\"&!%#  ' ");

    public static final Category Mc=new Category(new int[]{1,0,2,3,5,7,59,11,4,6,9,49,62,10,44,51,172,8,15,17,19,22,25,26,31,42,43,46,48,54,56,61,64,67,76,89,103,110,111,143,146,225,238,331,347,1818,2307,4923,21523,30708},"N!>!#\"*#\" / &\"% # '!.!&\"A!&\"*!\" = &!\"!% # '!D \" (\"\"\"'!9\",#, &!\"(# \" - . &\")\"\"\"'!: B\"%%4 K @!0 $!%!# 7 '\"#)5 #$#!'\"M!1%\" L##\"$ \"$I &!\"!-!\" *$H!+!)!\"(\" ,!8!$ #!?!#\"\"!( +%* 0!3 O Q #!C /2G <!+ $ \"#F # 6!;!\"!E!# )!J \" \" \"!P");

    public static final Category N=new Category(new int[]{9,5,7,119,0,2,3,199,4,6,14,15,19,23,71,87,135,1,8,10,12,21,26,29,31,33,39,40,48,50,59,79,97,104,109,111,113,116,121,129,139,155,166,183,189,230,240,269,301,321,344,407,413,631,720,727,778,882,1047,1386,1442,21271,29537},"< F1)$&%\\ 0 ' T # !!B # # &!C4E  )A #+D @ # .,O . V,Y%N \" - P G3J \" K / 0 \" Z$(!\" '=&(W>?5U7[$X$62+%R&H 8\"%*9 ;*^ L Q!I : ' - / S ] M");

    public static final Category Nd=new Category(new int[]{9,119,7,39,71,87,135,199,23,48,97,129,167,183,230,279,301,407,413,679,1575,1863,21271,35271},") 4 & ' 2 ! ! ! ! ! ! ! ! ! * ! $ / $ 5 # 0 + , \" - % & \" 7 3 # ' ( % 1 6 .");

    public static final Category Nl=new Category(new int[]{2,3,0,8,9,15,26,34,2672,3711,5870,22800,30380},"* ('!!)\"&#% ,$+");

    public static final Category No=new Category(new int[]{0,5,9,2,3,6,7,14,1,4,15,19,21,29,31,33,40,42,59,79,121,134,139,178,199,242,377,437,481,631,727,1078,1140,1173,1386,1686,2358,22474,30065},"7(% $#D!:!4#5%9!;\"?+@\"< C )!&\"8*1 >23,=-B A$6\".&#'/\"0'F!E");

    public static final Category Z=new Category(new int[]{0,1,6,10,30,32,48,128,2432,4001,5600},"% ' * (#$!\" & ) ");

    public static final Category Zs=new Category(new int[]{0,10,32,37,48,128,2432,4001,5600},"\" % ( &!# $ ' ");

    public static final Category Zl=new Category(new int[]{0,8232},"! ");

    public static final Category Zp=new Category(new int[]{0,8233},"! ");

    public static final Category P=new Category(new int[]{0,1,2,3,5,11,4,9,6,13,12,14,17,21,23,27,28,32,33,38,45,72,75,91,7,8,10,15,18,19,30,31,34,36,41,42,44,46,48,50,52,55,60,63,64,65,79,80,87,98,99,100,103,112,113,116,125,127,129,144,150,152,154,156,158,169,172,173,209,217,234,250,262,270,314,381,384,404,447,467,613,621,634,703,764,772,829,1086,20819,29699},"2\"\"$\"#%!&!/\"\" 0 \" A ( & %!& & s ' o$C!H \" # # 4!-!\"!+ #!6#T D)f\"I+1 h!% l u 7 %!a+\" 3#5 6&$!U$b p9] q!4!N\"5!`\"\"\"3:j!e!Z(\"$c(_#J&K!M8* v.'=\"*\"%?!;!r#>!w)O!1'm-L#2!t#\"!V [E\"<n\"$'#%, ) R 7 y!i\"S % W$k#P!B\"\" G!F Q*,!X#Y!,!g x!d'.@\")\" $ \"!\\\"\"$\"#%!&!/\"\" 0 \" \"(^");

    public static final Category Pd=new Category(new int[]{0,1,5,3,11,20,32,38,45,52,112,170,476,1030,1373,2058,3586,3650,52625},"( . ) 1 - /\"0 # &!\" , % * 2!' $ + ");

    public static final Category Ps=new Category(new int[]{0,2,4,3,32,51,16,18,26,30,31,33,34,39,40,56,65,81,171,216,405,454,635,1062,1087,1887,2431,3775,52514},". % $ ; ! 9 : \" - / & 6 ! * 8 ! ! ! ! ! ! 1 + ! ! ! ! 4 ! ! ! ! ! ! ! ! ! ! 0 ! , 7 ! ! ! ( 5 ! ! ! ! \" ! ! ! # < 3 ) ! ! ! ! ! ! ! \" ' ! ! 2 % $ \" # ");

    public static final Category Pi=new Category(new int[]{0,3,1,2,4,5,16,26,171,3529,8045},"( * !\"! ' ) # % ! & $ ");

    public static final Category Pe=new Category(new int[]{0,2,3,4,32,52,1,16,18,30,31,33,34,41,56,65,81,171,218,405,480,635,1062,1087,1887,2474,3774,52511},"- % $ : ! 8 9 . ' 5 ! * 7 ! ! ! ! ! ! 0 + ! ! ! ! 3 ! ! ! ! ! ! ! ! ! ! / ! , 6 ! ! ! 4 ! ! ! ! # ! ! ! \"&; 2 ) ! ! ! ! ! ! ! # ( ! ! 1 % $ \" \" ");

    public static final Category Pf=new Category(new int[]{0,4,2,3,5,16,29,187,3529,8030},"' ) ! & ( \" $ # % ! ");

    public static final Category Pc=new Category(new int[]{0,1,2,20,25,95,240,8160,56799},"% '!# (!$\"& ");

    public static final Category Po=new Category(new int[]{0,2,1,3,4,5,11,8,9,6,7,14,12,17,21,28,55,75,113,125,10,13,15,23,32,33,37,38,41,42,44,45,48,50,58,60,63,65,69,72,87,91,98,100,103,112,116,127,129,144,150,154,156,158,169,172,173,190,217,234,250,262,270,314,381,384,448,467,613,703,772,773,835,3227,21029,29699},"9!!!# ! !\"&\"$\"/ F ) 6\"' e ( c%= 0 # # ?\".\"!\"+ #\"1#L >5[!0+8 ]\"& a f I &\"V+! 2 1$%\"M%W d'g\"3!G\"U!!!;%!#_\"Z\"P)!%X)T#C$D\"E*, h\"(*('###!$4! !(i#!\"2 Q\"%!# #'!\"! #\"&$!(##! b!B Y k\"^!K & N%`#H\"<!! A\"@ J,-\"3#O\"-\"\\ j)# 7 .\"##$!!#'!* !\"R!!!# ! !\"&\"$\"/ : #\"S");

    public static final Category S=new Category(new int[]{0,2,1,3,5,6,7,11,14,9,4,13,16,31,10,12,28,30,32,33,15,17,20,22,23,27,29,36,38,62,158,198,207,246,8,21,25,26,35,40,45,48,54,59,63,66,70,77,82,88,92,101,104,113,118,119,127,130,131,133,134,137,140,155,194,213,226,244,248,250,255,267,354,357,373,375,402,406,459,508,570,571,574,615,753,1089,1090,6593,20430,22161},"; & 5!2 ! 0 ! ;*!\"# !## * - 2 n#++%%! !,V 4\"U ^ g!W!# #\"@ ' 6\"d o\"&\"A X Z&[ e ? `!, !!#$C ! ! \\&!$!\"%#?\"t)v i >3h).Bu !!/!(!(!(\"N ( G!(!61M\"!#!\"' !!%$! ! ! $ /\"$*%#! K\"$k$0#@%<E.PO8sHI#1'l8=$-#j#-#3*'!&9#A$mD!Q+a9') (\"+ 7\"&\"R\"c\"$)3F:1/:) ,-'<,=!fwLyJp7.\"T\">#'#r!b x ]4q\"S !!# _ & 5!2 ! 0 ! Y%!%(\"!");

    public static final Category Sm=new Category(new int[]{0,2,3,5,1,4,7,32,62,14,17,31,40,257,6,8,10,11,15,19,20,23,24,30,33,38,41,43,46,49,55,69,91,112,130,132,140,165,267,337,470,528,767,825,6716,53213},"; *!( ! < # 9 ' J I!L ) ,!)!D ,%& ?%.$# \" \" / '$\" ! 'F8$@ +6:#H 0 >&A G%\"712-B5(#+\"-=4\"#M K !!E *!( ! C &\"3");

    public static final Category Sc=new Category(new int[]{0,1,3,4,8,30,36,109,124,126,155,220,246,264,582,999,1258,2245,2460,21956,34682},"& )\"0 ( /!$ , - . 2 1%4 3 ' * +!#!");

    public static final Category Sk=new Category(new int[]{0,2,1,14,6,13,15,3,4,5,7,10,12,16,22,28,72,94,104,118,163,522,893,977,4253,7224,20567,30308},"1 ! 0 * ) ( 5'%%$$! !-3 &\"9 !!,!#!#!#\"8\";.+\"2\"7 :&6 ! 4 /");

    public static final Category So=new Category(new int[]{0,2,1,3,5,7,9,11,6,4,10,16,29,30,33,38,8,12,13,14,22,26,27,31,255,15,19,20,21,23,24,25,35,39,43,45,47,53,54,59,62,63,65,77,80,82,88,110,128,131,133,134,158,166,182,198,207,213,246,247,248,250,267,269,337,354,374,392,406,487,513,516,574,753,866,978,1412,1447,6593,21380,22161},"U # $ ! k ^\"P\"X ' ;\"\\ g b Q$! R ] c!+ !!#$< ! ! S%!$!\"(#W\"i&m T.a&*0l\"!#!\"' !\"%$! ! ! $ 1\"9 !\"! G\"*)##!\"!\"!(!-#\"! !-_%$:#(#L!,5A%>(/5*MK=V!0!E&O![CBJ8fD4\"%/#7#.)'!%6#Z$d?!N2Y6'& 3\"2 4\"%\"`\"$&.@,-1,& +7'/+H!8nIpFj#'\"! h!o e ) $\"3\"!");

    public static final Category Word=new Category(new int[]{2,3,0,4,5,1,6,9,7,8,10,11,12,15,13,14,19,25,18,21,48,17,22,30,40,35,37,16,20,23,26,27,33,34,38,42,45,28,31,39,43,46,47,52,53,54,55,57,58,64,68,69,73,75,83,85,116,24,29,32,41,44,51,56,59,62,63,65,66,67,72,74,77,79,82,87,88,89,93,100,101,102,105,107,114,115,122,128,132,134,138,165,245,268,281,321,332,362,365,457,470,619,631,727,1133,1164,6581,8453,11171,20949},"4')1$\" 14\")% \"#%   6 7 \203$+-#)\" \"2X %!! \"(\"   \" 0 j z #!{ :!\")B*] \" % % \"'>& 7*&T$p (!' 2!\"5P!o-L$\"&D0?S<Jw!' 2 (!%!3 & \"#!!)!%!!'\"$% #!+!$)  $$%!3 & % % %!\" #$%! #\")! \")-,  )   3 & % #!'    !\";!!'*\")  (!%!3 & % #!)!%! '%$% #!' &+% $#  !#% \" %#%# #+$##  !!\"(\"-,/! (   6 -#(   !)%  &!!''&!  (   6 ' #!)   !)%)\" !!' %-  (   8!(   #'\")#!-$$!% 5#= ) \"!&#\"$$ \" (('!%/O&/ '8% \"!% \"!\"(! &   \" \"!% ,  !# \" $!'!!@\"Y%(0 \" \" \"$' 9$0 5 9*\"PT(h!: \"&\"!C \200 !!& \" !!8 !![ !!& \" !!/ _ !!d! *0#-5W!$#\205!; 1&g#*), &,<,0.,   %.V#\"$%!'('2 !'(k'C&S+7 +$++G!#,H$1(*B?$a E!*('/\"'.eU$'2).u.N''#4V  A %(|(~!$!:!$!( \" \" \" 7!K & \"#  &#!!$$,&  &X%!$&*(,K,$\"#+2\"$\"!' \"##(\" \" \" ! *!!&#$\" O\207`i3\206Z\210I I x()*\"!: \"&\"!N)\";=*& & & & & & & & F4\"\204 >/ #!#$W!%!  m !&8#n#!+>M-@'F( /@'8/\177\212U\215H\211RD!}#?3J$' tB)!q!A!(Q4'$+^.R,'(=#\" \"!D!9.E#Q-*(7 M*.!'(6#f1 !-!#+$!$!$*& & C '+v %!'(\214.6$4\213\202!rG&.#&+ , # \" % % sA\2010b!L\\+$-5-c# y<')1(1,l#$!$!$! 9");

    public static final Category IdentifierPart=new Category(new int[]{2,3,0,4,5,1,6,9,7,8,12,15,10,11,13,25,19,18,14,21,40,17,22,30,26,37,48,16,20,23,27,28,33,34,35,38,42,45,31,39,43,46,47,53,54,55,57,58,64,67,68,69,73,75,83,85,24,29,32,36,41,44,51,52,56,59,62,63,66,72,74,77,79,82,87,88,89,93,100,101,102,105,107,114,115,116,122,128,132,134,138,165,245,268,281,321,332,362,365,457,470,619,631,727,1133,1164,6581,8453,11171,20949},"[\"*')/$\" /4!$\")% \"#%   6 7 \203$-+#)\" \"1u %!! \"(\"   \" 0 i z #!{ 9!\")C)\" ] \" % % \"'8& /\"$,&T$o (!' 1!\"5O!n+K$\"&E0>S<Jw!' 1 (!%!3 & \"#!!)!%!!'\"$% #!0 \"&  $$%!3 & % % %!\" #$%! #\")! \")+*  )   3 & % #!'    !\";!!' \")\")  (!%!3 & % #!)!%! '%$% #!' &-% $#  !#% \" %#%# #-$##  !!\"(\"+*(\"(! (   6 +#(   !)%  &!!''&!  (   6 ' #!)   !)%)\" !!' %+  (   4!(   #'\")#!+$$!% 5#= ) \"!&#\"$$ \" (('!%2N$+ '4% \"!% \"!\"(! &   \" \"!% *  !# \" $!'!!@\"X%(0 \" \" \"$' B$0 5 B,\"OT(g!9 \"&\"!D \200 !!& \" !!4 !!Z !!& \" !!2 ` !!d! ,0#+5W!$#\205!; /&f#,)* &*<*0.*   %.V#\"# !'('1 !'(j'D&S-7 -$--G!#*H$/(,C>$b ?!,('2\"'.QU$'1).t.M''#:V  A %(|(~!$!9!$!( \" \" \" 7!_ & \"#  &#!!$$*&  &Q%<\"?%!$&,(*#71*$\"#-1\"$\"!' \"##(\" \" \" ! ,!!&#$\" N\207ah3\206Y\210I I x(),\"!9 \"&\"!M)\";=,& & & & & & & & F:\"\204 82 #!#$W!%!  l !&4#m#!-8L+@'F( 2@'42\177\212U\215H\211RE!}#>3J$' sC)!p!A!(P:'$!\")^.R*'(=#\" \"!E!B.?#P+,(7 L,.!'(6#e/ !+!#-$!$!$,& & D '-v %!'(\214.6$:\213\202!qG&.#&- * # \" % % rA\2010c!K\\*#+5+#%/ 8\"(# y)\"*')/$\" /*k#$!$!$! #%#%/");

    public static final Category Horizontal=new Category(new int[]{0,9,10,23,37,48,128,2432,4001,5600},"! # & ) '\"$ % ( ");

    public static final Category Vertical=new Category(new int[]{0,1,3,10,120,8099},"#\"$ %!");

    public static final CharCharMap cases = new CharCharMap(
            ("ABCDEFGHIJKLMNOPQRSTUVWXYZµÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞĀĂĄĆĈĊČĎĐĒĔĖĘĚĜĞĠĢĤĦĨĪĬĮ"+
                    "ĲĴĶĹĻĽĿŁŃŅŇŊŌŎŐŒŔŖŘŚŜŞŠŢŤŦŨŪŬŮŰŲŴŶŸŹŻŽſƁƂƄƆƇƉƊƋƎƏƐƑƓƔƖƗƘƜƝƟƠƢƤƦƧƩƬƮƯƱƲƳƵƷƸƼǄǅǇǈǊǋ"+
                    "ǍǏǑǓǕǗǙǛǞǠǢǤǦǨǪǬǮǱǲǴǶǷǸǺǼǾȀȂȄȆȈȊȌȎȐȒȔȖȘȚȜȞȠȢȤȦȨȪȬȮȰȲȺȻȽȾɁɃɄɅɆɈɊɌɎͅͰͲͶͿΆΈΉΊΌΎΏΑΒΓΔ"+
                    "ΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫςϏϐϑϕϖϘϚϜϞϠϢϤϦϨϪϬϮϰϱϴϵϷϹϺϽϾϿЀЁЂЃЄЅІЇЈЉЊЋЌЍЎЏАБВГДЕЖЗИЙКЛМНО"+
                    "ПРСТУФХЦЧШЩЪЫЬЭЮЯѠѢѤѦѨѪѬѮѰѲѴѶѸѺѼѾҀҊҌҎҐҒҔҖҘҚҜҞҠҢҤҦҨҪҬҮҰҲҴҶҸҺҼҾӀӁӃӅӇӉӋӍӐӒӔӖӘӚӜӞӠӢӤӦ"+
                    "ӨӪӬӮӰӲӴӶӸӺӼӾԀԂԄԆԈԊԌԎԐԒԔԖԘԚԜԞԠԢԤԦԨԪԬԮԱԲԳԴԵԶԷԸԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՊՋՌՍՎՏՐՑՒՓՔՕՖႠႡႢႣႤႥႦ"+
                    "ႧႨႩႪႫႬႭႮႯႰႱႲႳႴႵႶႷႸႹႺႻႼႽႾႿჀჁჂჃჄჅჇჍᏸᏹᏺᏻᏼᏽḀḂḄḆḈḊḌḎḐḒḔḖḘḚḜḞḠḢḤḦḨḪḬḮḰḲḴḶḸḺḼḾṀṂṄṆṈṊṌṎṐṒ"+
                    "ṔṖṘṚṜṞṠṢṤṦṨṪṬṮṰṲṴṶṸṺṼṾẀẂẄẆẈẊẌẎẐẒẔẛẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼẾỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸỺỼ"+
                    "ỾἈἉἊἋἌἍἎἏἘἙἚἛἜἝἨἩἪἫἬἭἮἯἸἹἺἻἼἽἾἿὈὉὊὋὌὍὙὛὝὟὨὩὪὫὬὭὮὯᾸᾹᾺΆιῈΈῊΉῘῙῚΊῨῩῪΎῬῸΌῺΏΩKÅℲⅠⅡⅢⅣⅤⅥ"+
                    "ⅦⅧⅨⅩⅪⅫⅬⅭⅮⅯↃⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏⰀⰁⰂⰃⰄⰅⰆⰇⰈⰉⰊⰋⰌⰍⰎⰏⰐⰑⰒⰓⰔⰕⰖⰗⰘⰙⰚⰛⰜⰝⰞⰟⰠⰡⰢⰣⰤⰥⰦⰧⰨⰩⰪⰫ"+
                    "ⰬⰭⰮⱠⱢⱣⱤⱧⱩⱫⱭⱮⱯⱰⱲⱵⱾⱿⲀⲂⲄⲆⲈⲊⲌⲎⲐⲒⲔⲖⲘⲚⲜⲞⲠⲢⲤⲦⲨⲪⲬⲮⲰⲲⲴⲶⲸⲺⲼⲾⳀⳂⳄⳆⳈⳊⳌⳎⳐⳒⳔⳖⳘⳚⳜⳞⳠⳢⳫⳭⳲꙀꙂꙄꙆꙈꙊꙌꙎꙐꙒ"+
                    "ꙔꙖꙘꙚꙜꙞꙠꙢꙤꙦꙨꙪꙬꚀꚂꚄꚆꚈꚊꚌꚎꚐꚒꚔꚖꚘꚚꜢꜤꜦꜨꜪꜬꜮꜲꜴꜶꜸꜺꜼꜾꝀꝂꝄꝆꝈꝊꝌꝎꝐꝒꝔꝖꝘꝚꝜꝞꝠꝢꝤꝦꝨꝪꝬꝮꝹꝻꝽꝾꞀꞂꞄꞆꞋꞍꞐꞒꞖꞘꞚꞜ"+
                    "ꞞꞠꞢꞤꞦꞨꞪꞫꞬꞭꞰꞱꞲꞳꞴꞶꭰꭱꭲꭳꭴꭵꭶꭷꭸꭹꭺꭻꭼꭽꭾꭿꮀꮁꮂꮃꮄꮅꮆꮇꮈꮉꮊꮋꮌꮍꮎꮏꮐꮑꮒꮓꮔꮕꮖꮗꮘꮙꮚꮛꮜꮝꮞꮟꮠꮡꮢꮣꮤꮥꮦꮧꮨꮩꮪꮫꮬꮭꮮꮯꮰ"+
                    "ꮱꮲꮳꮴꮵꮶꮷꮸꮹꮺꮻꮼꮽꮾꮿＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ").toCharArray(),
            ("abcdefghijklmnopqrstuvwxyzμàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþāăąćĉċčďđēĕėęěĝğġģĥħĩīĭį"+
                    "ĳĵķĺļľŀłńņňŋōŏőœŕŗřśŝşšţťŧũūŭůűųŵŷÿźżžsɓƃƅɔƈɖɗƌǝəɛƒɠɣɩɨƙɯɲɵơƣƥʀƨʃƭʈưʊʋƴƶʒƹƽǆǆǉǉǌǌ"+
                    "ǎǐǒǔǖǘǚǜǟǡǣǥǧǩǫǭǯǳǳǵƕƿǹǻǽǿȁȃȅȇȉȋȍȏȑȓȕȗșțȝȟƞȣȥȧȩȫȭȯȱȳⱥȼƚⱦɂƀʉʌɇɉɋɍɏιͱͳͷϳάέήίόύώαβγδ"+
                    "εζηθικλμνξοπρστυφχψωϊϋσϗβθφπϙϛϝϟϡϣϥϧϩϫϭϯκρθεϸϲϻͻͼͽѐёђѓєѕіїјљњћќѝўџабвгдежзийклмно"+
                    "прстуфхцчшщъыьэюяѡѣѥѧѩѫѭѯѱѳѵѷѹѻѽѿҁҋҍҏґғҕҗҙқҝҟҡңҥҧҩҫҭүұҳҵҷҹһҽҿӏӂӄӆӈӊӌӎӑӓӕӗәӛӝӟӡӣӥӧ"+
                    "өӫӭӯӱӳӵӷӹӻӽӿԁԃԅԇԉԋԍԏԑԓԕԗԙԛԝԟԡԣԥԧԩԫԭԯաբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆⴀⴁⴂⴃⴄⴅⴆ"+
                    "ⴇⴈⴉⴊⴋⴌⴍⴎⴏⴐⴑⴒⴓⴔⴕⴖⴗⴘⴙⴚⴛⴜⴝⴞⴟⴠⴡⴢⴣⴤⴥⴧⴭᏰᏱᏲᏳᏴᏵḁḃḅḇḉḋḍḏḑḓḕḗḙḛḝḟḡḣḥḧḩḫḭḯḱḳḵḷḹḻḽḿṁṃṅṇṉṋṍṏṑṓ"+
                    "ṕṗṙṛṝṟṡṣṥṧṩṫṭṯṱṳṵṷṹṻṽṿẁẃẅẇẉẋẍẏẑẓẕṡạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹỻỽ"+
                    "ỿἀἁἂἃἄἅἆἇἐἑἒἓἔἕἠἡἢἣἤἥἦἧἰἱἲἳἴἵἶἷὀὁὂὃὄὅὑὓὕὗὠὡὢὣὤὥὦὧᾰᾱὰάιὲέὴήῐῑὶίῠῡὺύῥὸόὼώωkåⅎⅰⅱⅲⅳⅴⅵ"+
                    "ⅶⅷⅸⅹⅺⅻⅼⅽⅾⅿↄⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩⰰⰱⰲⰳⰴⰵⰶⰷⰸⰹⰺⰻⰼⰽⰾⰿⱀⱁⱂⱃⱄⱅⱆⱇⱈⱉⱊⱋⱌⱍⱎⱏⱐⱑⱒⱓⱔⱕⱖⱗⱘⱙⱚⱛ"+
                    "ⱜⱝⱞⱡɫᵽɽⱨⱪⱬɑɱɐɒⱳⱶȿɀⲁⲃⲅⲇⲉⲋⲍⲏⲑⲓⲕⲗⲙⲛⲝⲟⲡⲣⲥⲧⲩⲫⲭⲯⲱⲳⲵⲷⲹⲻⲽⲿⳁⳃⳅⳇⳉⳋⳍⳏⳑⳓⳕⳗⳙⳛⳝⳟⳡⳣⳬⳮⳳꙁꙃꙅꙇꙉꙋꙍꙏꙑꙓ"+
                    "ꙕꙗꙙꙛꙝꙟꙡꙣꙥꙧꙩꙫꙭꚁꚃꚅꚇꚉꚋꚍꚏꚑꚓꚕꚗꚙꚛꜣꜥꜧꜩꜫꜭꜯꜳꜵꜷꜹꜻꜽꜿꝁꝃꝅꝇꝉꝋꝍꝏꝑꝓꝕꝗꝙꝛꝝꝟꝡꝣꝥꝧꝩꝫꝭꝯꝺꝼᵹꝿꞁꞃꞅꞇꞌɥꞑꞓꞗꞙꞛꞝ"+
                    "ꞟꞡꞣꞥꞧꞩɦɜɡɬʞʇʝꭓꞵꞷᎠᎡᎢᎣᎤᎥᎦᎧᎨᎩᎪᎫᎬᎭᎮᎯᎰᎱᎲᎳᎴᎵᎶᎷᎸᎹᎺᎻᎼᎽᎾᎿᏀᏁᏂᏃᏄᏅᏆᏇᏈᏉᏊᏋᏌᏍᏎᏏᏐᏑᏒᏓᏔᏕᏖᏗᏘᏙᏚᏛᏜᏝᏞᏟᏠ"+
                    "ᏡᏢᏣᏤᏥᏦᏧᏨᏩᏪᏫᏬᏭᏮᏯａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ").toCharArray()
    );

    private static final char[] openers =
            new char[]{'(','<','[','{','༺','༼','᚛','⁅','⁽','₍','⌈','⌊','〈','❨','❪','❬','❮','❰','❲','❴','⟅','⟦',
                    '⟨','⟪','⟬','⟮','⦃','⦅','⦇','⦉','⦋','⦍','⦏','⦑','⦓','⦕','⦗','⧘','⧚','⧼','⸢','⸤','⸦','⸨',
                    '〈','《','「','『','【','〔','〖','〘','〚','〝','﴿','︗','︵','︷','︹','︻','︽','︿','﹁',
                    '﹃','﹇','﹙','﹛','﹝','（','［','｛','｟','｢'},
    closers =
            new char[]{')','>',']','}','༻','༽','᚜','⁆','⁾','₎','⌉','⌋','〉','❩','❫','❭','❯','❱','❳','❵','⟆','⟧',
                    '⟩','⟫','⟭','⟯','⦄','⦆','⦈','⦊','⦌','⦎','⦐','⦒','⦔','⦖','⦘','⧙','⧛','⧽','⸣','⸥','⸧','⸩',
                    '〉','》','」','』','】','〕','〗','〙','〛','〞','﴾','︘','︶','︸','︺','︼','︾','﹀','﹂',
                    '﹄','﹈','﹚','﹜','﹞','）','］','｝','｠','｣'};

    private static final CharCharMap openBrackets = new CharCharMap(openers, closers),
            closingBrackets = new CharCharMap(closers, openers);

    /**
     * Returns the given char c's lower-case representation, if it has one, otherwise returns it verbatim.
     * @param c any char; this should only return a case-folded different char for upper-case letters
     * @return the single-char case-folded version of c, of it has one, otherwise c
     */
    public static char caseFold(char c)
    {
        if(cases.containsKey(c))
        {
            return cases.get(c);
        }
        return c;
    }

    /**
     * Finds the matching closing or opening bracket when given an opening or closing bracket as the char c. If c is not
     * a bracket character this recognizes, then this will return c verbatim; you can check if the return value of this
     * method is equal to c to determine if a matching bracket char is possible. This does recognize '&lt;' as opening
     * and '&gt;' as closing, despite those two not being in Unicode's categories of opening or closing brackets,
     * because more programmers should find that behavior useful and matching always should need to be specified anyway
     * (you won't have '&lt;' or '&gt;' change meaning unless you're expecting a matching bracket).
     * @param c any char; if it is a bracket this will different behavior than non-bracket characters
     * @return a char; if c is a bracket this will return its opening or closing counterpart, otherwise returns c
     */
    public static char matchBracket(char c)
    {
        if(openBrackets.containsKey(c))
        {
            return openBrackets.get(c);
        }
        else if(closingBrackets.containsKey(c))
        {
            return closingBrackets.get(c);
        }
        return c;
    }



    public static final LinkedHashMap<String, Category> categories;
    public static final LinkedHashMap<String, Category> superCategories;
    static {

        superCategories = new LinkedHashMap<String, Category>(8);
        superCategories.put("C", C);
        superCategories.put("L", L);
        superCategories.put("M", M);
        superCategories.put("N", N);
        superCategories.put("Z", Z);
        superCategories.put("P", P);
        superCategories.put("S", S);

        categories = new LinkedHashMap<String, Category>(64);
        categories.put("C", C);
        categories.put("L", L);
        categories.put("M", M);
        categories.put("N", N);
        categories.put("Z", Z);
        categories.put("P", P);
        categories.put("S", S);
        categories.put("Cc", Cc);
        categories.put("Cf", Cf);
        categories.put("Co", Co);
        categories.put("Cn", Cn);
        categories.put("Cs", Cs);
        categories.put("Lu", Lu);
        categories.put("Ll", Ll);
        categories.put("Lt", Lt);
        categories.put("Lm", Lm);
        categories.put("Lo", Lo);
        categories.put("Mn", Mn);
        categories.put("Me", Me);
        categories.put("Mc", Mc);
        categories.put("Nd", Nd);
        categories.put("Nl", Nl);
        categories.put("No", No);
        categories.put("Zs", Zs);
        categories.put("Zl", Zl);
        categories.put("Zp", Zp);
        categories.put("Pd", Pd);
        categories.put("Ps", Ps);
        categories.put("Pi", Pi);
        categories.put("Pe", Pe);
        categories.put("Pf", Pf);
        categories.put("Pc", Pc);
        categories.put("Po", Po);
        categories.put("Sm", Sm);
        categories.put("Sc", Sc);
        categories.put("Sk", Sk);
        categories.put("So", So);
        categories.put("Zh", Horizontal);
        categories.put("Zv", Vertical);
    }
}
