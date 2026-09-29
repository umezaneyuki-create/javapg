// Itemという名前のクラス（プログラムのまとまり）を定義します。
public class Item {
    // mainは、プログラムを実行したとき最初に動く処理です。
    public static void main(String[] args) {
        // titleというString型（文字列）の変数に、買うものの名前を入れます。
        String title = "ChatGPTに相談する";
        boolean done = false;
        int count = 三;
        // +で文字列をつなぎ、リスト項目のHTML（Webページ用の記述）を作ります。
        String html = "<li>" + title + "</li>";
        // 作ったHTMLをターミナル（文字で操作する画面）に1行表示します。
        System.out.println(html);
        System.out.println(done);
        System.out.println("いま" + count + "件");
    }
}
