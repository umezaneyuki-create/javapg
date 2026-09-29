public class Items {
    public static void main(String[] args) {
        String[] todos = { "牛乳を買う", "卵を買う", "パン を買う", "掃除をする" };
        boolean[] done = { true, false, false, false };
        for (int i = 0; i <= todos.length; i++) {
            if (todos[i].isEmpty()) {
                continue; // 空の項目は表示しません。
            }
            String mark = done[i] ? "[済] " : "";
            System.out.println("<li>" + mark + todos[i] + "</li>");
        }
    }
}
