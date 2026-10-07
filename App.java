import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App {
    private static Connection db; // ★ SQLite????

    private static void initializeDatabase() throws SQLException { // ★ Add and migrate deadline column
        db = DriverManager.getConnection("jdbc:sqlite:todos.db"); // ★ Add and migrate deadline column
        try (PreparedStatement s = db.prepareStatement("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER, deadline TEXT, registered_date TEXT, start_date TEXT, deleted INTEGER DEFAULT 0)")) { // ★ Add and migrate deadline column
            s.executeUpdate(); // ★ Add and migrate deadline column
        }
        boolean hasDeadline = false; // ★ Add and migrate deadline column
        try (PreparedStatement s = db.prepareStatement("PRAGMA table_info(todos)"); ResultSet rs = s.executeQuery()) { // ★ Add and migrate deadline column
            while (rs.next()) { // ★ Add and migrate deadline column
                if ("deadline".equals(rs.getString("name"))) hasDeadline = true; // ★ Add and migrate deadline column
            }
        }
        if (!hasDeadline) { // ★ Add and migrate deadline column
            try (PreparedStatement s = db.prepareStatement("ALTER TABLE todos ADD COLUMN deadline TEXT")) { // ★ Add and migrate deadline column
                s.executeUpdate(); // ★ Add and migrate deadline column
            }
        }
        boolean hasRegisteredDate = false;
        boolean hasStartDate = false;
        try (PreparedStatement s = db.prepareStatement("PRAGMA table_info(todos)"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                if ("registered_date".equals(rs.getString("name"))) hasRegisteredDate = true;
                if ("start_date".equals(rs.getString("name"))) hasStartDate = true;
            }
        }
        if (!hasRegisteredDate) {
            try (PreparedStatement s = db.prepareStatement("ALTER TABLE todos ADD COLUMN registered_date TEXT")) { s.executeUpdate(); }
        }
        if (!hasStartDate) {
            try (PreparedStatement s = db.prepareStatement("ALTER TABLE todos ADD COLUMN start_date TEXT")) { s.executeUpdate(); }
        }
        boolean hasDeleted = false;
        try (PreparedStatement s = db.prepareStatement("PRAGMA table_info(todos)"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) if ("deleted".equals(rs.getString("name"))) hasDeleted = true;
        }
        if (!hasDeleted) {
            try (PreparedStatement s = db.prepareStatement("ALTER TABLE todos ADD COLUMN deleted INTEGER DEFAULT 0")) { s.executeUpdate(); }
        }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deleted = 0 WHERE deleted IS NULL")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET registered_date = date('now','localtime') WHERE registered_date IS NULL OR registered_date = ''")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET start_date = '' WHERE start_date IS NULL")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deadline = '10/13' WHERE deadline IS NULL OR deadline = ''")) { // ★ Add and migrate deadline column
            s.executeUpdate(); // ★ Add and migrate deadline column
        }
    }

    private static synchronized void addTodo(String title, String startDate, String deadline) throws SQLException { // ★ SQLite????
        try (PreparedStatement s = db.prepareStatement("INSERT INTO todos (title, done, registered_date, start_date, deadline) VALUES (?, 0, date('now','localtime'), ?, ?)")) { // ★ SQLite????
            s.setString(1, title); // ★ SQLite????
            s.setString(2, startDate);
            s.setString(3, deadline);
            s.executeUpdate(); // ★ SQLite????
        }
    }

    private static synchronized void markDone(int id) throws SQLException { // ★ SQLite????
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET done = 1 WHERE id = ?")) { // ★ SQLite????
            s.setInt(1, id); // ★ SQLite????
            s.executeUpdate(); // ★ SQLite????
        }
    }

    private static synchronized void updateTodo(int id, String title, String startDate, String deadline) throws SQLException { // ★ Edit Todo title
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET title = ?, start_date = ?, deadline = ? WHERE id = ?")) { // ★ Edit Todo title
            s.setString(1, title); // ★ Edit Todo title
            s.setString(2, startDate);
            s.setString(3, deadline);
            s.setInt(4, id);
            s.executeUpdate(); // ★ Edit Todo title
        }
    }

    private static synchronized Todo findTodo(int id) throws SQLException { // ★ Edit Todo title
        try (PreparedStatement s = db.prepareStatement("SELECT id, title, done, registered_date, start_date, deadline FROM todos WHERE id = ? AND deleted = 0")) { // ★ Edit Todo title
            s.setInt(1, id); // ★ Edit Todo title
            try (ResultSet rs = s.executeQuery()) { // ★ Edit Todo title
                if (rs.next()) return new Todo(rs.getInt("id"), rs.getString("title"), rs.getInt("done") != 0, rs.getString("registered_date"), rs.getString("start_date"), rs.getString("deadline")); // ★ Edit Todo title
            }
        }
        return null; // ★ Edit Todo title
    }

    private static synchronized void deleteTodo(int id) throws SQLException { // ★ SQLite????
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deleted = 1 WHERE id = ?")) { // ★ SQLite????
            s.setInt(1, id); // ★ SQLite????
            s.executeUpdate(); // ★ SQLite????
        }
    }

    private static synchronized void deleteVisibleCompleted(String titleSearch, String deadlineSearch) throws SQLException { // ★ Delete completed tasks currently visible
        StringBuilder sql = new StringBuilder("UPDATE todos SET deleted = 1 WHERE done = 1 AND deleted = 0"); // ★ Delete completed tasks currently visible
        if (!titleSearch.trim().isEmpty()) sql.append(" AND title LIKE ?"); // ★ Delete completed tasks currently visible
        if (!deadlineSearch.trim().isEmpty()) sql.append(" AND deadline LIKE ?"); // ★ Delete completed tasks currently visible
        try (PreparedStatement s = db.prepareStatement(sql.toString())) { // ★ Delete completed tasks currently visible
            int parameter = 1; // ★ Delete completed tasks currently visible
            if (!titleSearch.trim().isEmpty()) s.setString(parameter++, "%" + titleSearch + "%"); // ★ Delete completed tasks currently visible
            if (!deadlineSearch.trim().isEmpty()) s.setString(parameter++, "%" + deadlineSearch + "%"); // ★ Delete completed tasks currently visible
            s.executeUpdate(); // ★ Delete completed tasks currently visible
        }
    }

    private static synchronized List<Todo> loadTodos(String filter, String sort, String titleSearch, String deadlineSearch) throws SQLException { // ★ Search within selected tab
        List<Todo> todos = new ArrayList<>(); // ★ Search within selected tab
        StringBuilder condition = new StringBuilder(" WHERE deleted = 0"); // ★ Search within selected tab
        if ("todo".equals(filter)) condition.append(" AND done = 0 AND (start_date IS NULL OR TRIM(start_date) = '')");
        else if ("doing".equals(filter)) condition.append(" AND done = 0 AND start_date IS NOT NULL AND TRIM(start_date) <> ''");
        else if ("done".equals(filter)) condition.append(" AND done = 1");
        if (!titleSearch.trim().isEmpty()) condition.append(" AND ").append("title LIKE ?"); // ★ Search within selected tab
        if (!deadlineSearch.trim().isEmpty()) condition.append(" AND ").append("deadline LIKE ?"); // ★ Search within selected tab
        String direction = "desc".equals(sort) ? "DESC" : "ASC"; // ★ Search within selected tab
        String sql = "SELECT id, title, done, registered_date, start_date, deadline FROM todos" + condition + " ORDER BY id " + direction; // ★ Search within selected tab
        try (PreparedStatement s = db.prepareStatement(sql)) { // ★ Search within selected tab
            int parameter = 1; // ★ Search within selected tab
            if (!titleSearch.trim().isEmpty()) s.setString(parameter++, "%" + titleSearch + "%"); // ★ Search within selected tab
            if (!deadlineSearch.trim().isEmpty()) s.setString(parameter++, "%" + deadlineSearch + "%"); // ★ Search within selected tab
            try (ResultSet rs = s.executeQuery()) { // ★ Search within selected tab
                while (rs.next()) { // ★ Search within selected tab
                    todos.add(new Todo(rs.getInt("id"), rs.getString("title"), rs.getInt("done") != 0, rs.getString("registered_date"), rs.getString("start_date"), rs.getString("deadline"))); // ★ Search within selected tab
                }
            }
        }
        return todos; // ★ Search within selected tab
    }

    public static void main(String[] args) throws Exception {
        initializeDatabase(); // ★ SQLite????
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            try { // ★ SQLite????
                if (path.equals("/add") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String title = URLDecoder.decode(queryValue(body, "todo"), StandardCharsets.UTF_8);
                    String deadline = URLDecoder.decode(queryValue(body, "deadline"), StandardCharsets.UTF_8);
                    if (title.trim().isEmpty() || deadline.trim().isEmpty()) {
                        String error = title.trim().isEmpty() ? "todo" : "deadline";
                        exchange.getResponseHeaders().set("Location", "/?error=" + error + formValues(title, deadline));
                    } else {
                        addTodo(title, "", deadline);
                        exchange.getResponseHeaders().set("Location", "/");
                    }
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/edit") && method.equals("GET")) {
                    String query = exchange.getRequestURI().getQuery();
                    Todo todo = findTodo(requestedId(query));
                    if (todo == null) {
                        message = "Todo not found";
                    } else {
                        String filter = requestedFilter(query);
                        String sort = requestedSort(query);
                        String titleSearch = URLDecoder.decode(queryValue(query, "titleSearch"), StandardCharsets.UTF_8);
                        String deadlineSearch = URLDecoder.decode(queryValue(query, "deadlineSearch"), StandardCharsets.UTF_8);
                        StringBuilder form = new StringBuilder();
                        String error = queryValue(query, "error");
                        String formTitle = error.isEmpty() ? todo.getTitle() : URLDecoder.decode(queryValue(query, "todo"), StandardCharsets.UTF_8);
                        String formDeadline = error.isEmpty() ? todo.getDeadline() : URLDecoder.decode(queryValue(query, "deadline"), StandardCharsets.UTF_8);
                        String formStartDate = error.isEmpty() ? todo.getStartDate() : URLDecoder.decode(queryValue(query, "startDate"), StandardCharsets.UTF_8);
                        if ("todo".equals(error)) form.append("<p style='color:red;font-weight:bold'>\u300c\u3084\u308b\u3053\u3068\u300d\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044</p>");
                        if ("deadline".equals(error)) form.append("<p style='color:red;font-weight:bold'>\u300c\u7de0\u5207\u65e5\u300d\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044</p>");
                        form.append("<form method='post' action='/edit'><input type='hidden' name='id' value='").append(todo.getId()).append("'>");
                        form.append("<input type='hidden' name='titleSearch' value='").append(escapeHtml(titleSearch)).append("'><input type='hidden' name='deadlineSearch' value='").append(escapeHtml(deadlineSearch)).append("'>");
                        form.append("<label>\u3084\u308b\u3053\u3068<br><input name='todo' value='").append(escapeHtml(formTitle)).append("'></label> ");
                        form.append("<label>\u958b\u59cb\u65e5<br><input name='startDate' placeholder='10/01' value='").append(escapeHtml(formStartDate)).append("</label> ");
                        form.append("<label>\u7de0\u5207\u65e5<br><input name='deadline' placeholder='10/13' value='").append(escapeHtml(formDeadline)).append("'></label>");
                        form.append("<button>\u4fdd\u5b58</button></form>");
                        message = form.toString();
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    }
                } else if (path.equals("/edit") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    int id = requestedId(body);
                    String title = URLDecoder.decode(queryValue(body, "todo"), StandardCharsets.UTF_8);
                    String startDate = URLDecoder.decode(queryValue(body, "startDate"), StandardCharsets.UTF_8);
                    String deadline = URLDecoder.decode(queryValue(body, "deadline"), StandardCharsets.UTF_8);
                    String filter = requestedFilter(body);
                    String sort = requestedSort(body);
                    String searchParams = searchQuery(URLDecoder.decode(queryValue(body, "titleSearch"), StandardCharsets.UTF_8),
                            URLDecoder.decode(queryValue(body, "deadlineSearch"), StandardCharsets.UTF_8));
                    if (title.trim().isEmpty() || deadline.trim().isEmpty()) {
                        String error = title.trim().isEmpty() ? "todo" : "deadline";
                        exchange.getResponseHeaders().set("Location", "/edit?id=" + id + "&filter=" + filter + "&sort=" + sort + searchParams + "&error=" + error + formValues(title, deadline));
                    } else {
                        if (id > 0) updateTodo(id, title, startDate, deadline);
                        exchange.getResponseHeaders().set("Location", "/?filter=" + filter + "&sort=" + sort + searchParams);
                    }
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/delete-selected") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    for (String parameter : body.split("&")) {
                        int equals = parameter.indexOf('=');
                        if (equals >= 0 && "deleteIds".equals(URLDecoder.decode(parameter.substring(0, equals), StandardCharsets.UTF_8))) {
                            try { deleteTodo(Integer.parseInt(URLDecoder.decode(parameter.substring(equals + 1), StandardCharsets.UTF_8))); }
                            catch (NumberFormatException ignored) { }
                        }
                    }
                    exchange.getResponseHeaders().set("Location", listUrl(body));
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/delete-completed-visible") && method.equals("POST")) { // ★ Bulk delete visible completed tasks
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★ Bulk delete visible completed tasks
                    String titleSearch = URLDecoder.decode(queryValue(body, "titleSearch"), StandardCharsets.UTF_8); // ★ Bulk delete visible completed tasks
                    String deadlineSearch = URLDecoder.decode(queryValue(body, "deadlineSearch"), StandardCharsets.UTF_8); // ★ Bulk delete visible completed tasks
                    deleteVisibleCompleted(titleSearch, deadlineSearch); // ★ Bulk delete visible completed tasks
                    exchange.getResponseHeaders().set("Location", listUrl(body)); // ★ Bulk delete visible completed tasks
                    exchange.sendResponseHeaders(303, -1); // ★ Bulk delete visible completed tasks
                    exchange.close(); // ★ Bulk delete visible completed tasks
                    return; // ★ Bulk delete visible completed tasks
                } else if (path.equals("/done") && method.equals("GET")) {
                    String query = exchange.getRequestURI().getQuery();
                    int id = requestedId(query);
                    if (id > 0) markDone(id);
                    exchange.getResponseHeaders().set("Location", listUrl(query));
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/delete") && method.equals("GET")) {
                    String query = exchange.getRequestURI().getQuery();
                    int id = requestedId(query);
                    if (id > 0) deleteTodo(id);
                    exchange.getResponseHeaders().set("Location", listUrl(query));
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/")) {
                    String query = exchange.getRequestURI().getQuery();
                    String filter = requestedFilter(query);
                    String sort = requestedSort(query);
                    String titleSearch = URLDecoder.decode(queryValue(query, "titleSearch"), StandardCharsets.UTF_8);
                    String deadlineSearch = URLDecoder.decode(queryValue(query, "deadlineSearch"), StandardCharsets.UTF_8);
                    String searchParams = searchQuery(titleSearch, deadlineSearch);
                    String htmlSearchParams = searchParams.replace("&", "&amp;");
                    List<Todo> todos = loadTodos(filter, sort, titleSearch, deadlineSearch);
                    int doneCount = 0;
                    for (Todo todo : todos) {
                        if (todo.isDone()) doneCount++;
                    }
                    StringBuilder html = new StringBuilder();
                    String error = queryValue(query, "error");
                    String formTitle = error.isEmpty() ? titleSearch : URLDecoder.decode(queryValue(query, "todo"), StandardCharsets.UTF_8);
                    String formDeadline = error.isEmpty() ? deadlineSearch : URLDecoder.decode(queryValue(query, "deadline"), StandardCharsets.UTF_8);
                    if ("todo".equals(error)) html.append("<p style='color:red;font-weight:bold'>\u300c\u3084\u308b\u3053\u3068\u300d\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044</p>");
                    if ("deadline".equals(error)) html.append("<p style='color:red;font-weight:bold'>\u300c\u7de0\u5207\u65e5\u300d\u3092\u5165\u529b\u3057\u3066\u304f\u3060\u3055\u3044</p>");
                    html.append("<form method='post' action='/add' novalidate><div style='display:flex;gap:12px;align-items:end'>")
                        .append("<label>\u3084\u308b\u3053\u3068<br><input name='todo' value='").append(escapeHtml(formTitle)).append("'></label>")
                        .append("<label>\u7de0\u5207\u65e5<br><input name='deadline' placeholder='10/13' value='").append(escapeHtml(formDeadline)).append("'></label>")
                        .append("<button type='button' onclick='this.form.querySelector(&quot;[name=todo]&quot;).value=&quot;&quot;;this.form.querySelector(&quot;[name=deadline]&quot;).value=&quot;&quot;'>\u30af\u30ea\u30a2</button><button>\u4fdd\u5b58</button>")
                        .append("<button type='button' id='deleteModeButton' onclick=\"var boxes=document.querySelectorAll('.delete-selection');if(this.dataset.active!=='true'){boxes.forEach(function(b){b.style.display='inline'});this.dataset.active='true';this.textContent='\\u9078\\u629e\\u3057\\u305f\\u884c\\u3092\\u524a\\u9664';}else{var form=document.getElementById('deleteForm');if(!document.querySelector('.delete-selection:checked')){alert('\\u884c\\u3092\\u9078\\u629e\\u3057\\u3066\\u304f\\u3060\\u3055\\u3044');return;}document.getElementById('deleteCount').textContent=document.querySelectorAll('.delete-selection:checked').length;document.getElementById('deleteConfirm').style.display='flex';}\">\u524a\u9664</button>")
                        .append("<button type='button' onclick=\"window.location.href='/?filter=").append(filter).append("&amp;sort=").append(sort).append("&amp;titleSearch='+encodeURIComponent(this.form.todo.value)+'&amp;deadlineSearch='+encodeURIComponent(this.form.deadline.value)\">\u691c\u7d22</button></div></form>");
                    html.append("<div style='display:flex;justify-content:space-between;align-items:center;gap:12px'><div><a href='/?filter=all&amp;sort=desc").append(htmlSearchParams).append("'>\u5168\u90e8</a> | ")
                        .append("<a href='/?filter=todo&amp;sort=desc").append(htmlSearchParams).append("'>\u3084\u308b\u3053\u3068</a> | ")
                        .append("<a href='/?filter=doing&amp;sort=desc").append(htmlSearchParams).append("'>\u3084\u3063\u3066\u308b\u3053\u3068</a> | ")
                        .append("<a href='/?filter=done&amp;sort=desc").append(htmlSearchParams).append("'>\u3084\u3063\u305f\u3053\u3068</a></div>");
                    html.append("</div>");
                    if ("desc".equals(sort)) {
                        html.append("<p><a href='/?filter=").append(filter).append("&amp;sort=asc").append(htmlSearchParams).append("'>\u53e4\u3044\u9806</a></p>");
                    } else {
                        html.append("<p><a href='/?filter=").append(filter).append("&amp;sort=desc").append(htmlSearchParams).append("'>\u65b0\u3057\u3044\u9806</a></p>");
                    }
                    html.append("<form id='deleteForm' method='post' action='/delete-selected'>")
                        .append("<input type='hidden' name='titleSearch' value='").append(escapeHtml(titleSearch)).append("'><input type='hidden' name='deadlineSearch' value='").append(escapeHtml(deadlineSearch)).append("'></form>")
                        .append("<div style='display:grid;grid-template-columns:55px minmax(180px,2fr) 130px 130px 130px;gap:8px;font-weight:bold;border-bottom:1px solid #888;padding:6px 0'>")
                        .append("<span>\u2611</span><span>\u5185\u5bb9</span><span>\u767b\u9332\u65e5</span><span>\u958b\u59cb\u65e5</span><span>\u7de0\u5207\u65e5</span></div>")
                        .append("<ul style='list-style:none;padding-left:0'>");
                    for (Todo todo : todos) {
                        html.append("<li style='display:grid;grid-template-columns:55px minmax(180px,2fr) 130px 130px 130px;gap:8px;align-items:center;padding:5px 0'>")
                            .append("<span><input class='delete-selection' type='checkbox' name='deleteIds' value='").append(todo.getId()).append("' form='deleteForm' style='display:none'> <a href='/done?id=").append(todo.getId()).append("&amp;filter=").append(filter).append("&amp;sort=").append(sort).append(htmlSearchParams).append("'>").append(todo.isDone() ? "\u2611" : "\u2610").append("</a></span>")
                            .append("<span><a href='#' onclick=\"this.style.display='none';this.nextElementSibling.style.display='inline';return false\">").append(escapeHtml(todo.getTitle())).append("</a>")
                            .append("<form method='post' action='/edit' style='display:none'>")
                            .append("<input type='hidden' name='titleSearch' value='").append(escapeHtml(titleSearch)).append("'><input type='hidden' name='deadlineSearch' value='").append(escapeHtml(deadlineSearch)).append("'>")
                            .append("<input name='todo' value='").append(escapeHtml(todo.getTitle())).append("' style='width:180px'> ")
                            .append("<input name='startDate' value='").append(escapeHtml(todo.getStartDate())).append("' placeholder='\u958b\u59cb\u65e5' style='width:90px'> ")
                            .append("<input name='deadline' value='").append(escapeHtml(todo.getDeadline())).append("' placeholder='\u7de0\u5207\u65e5' style='width:90px'> ")
                            .append("<button>\u4fdd\u5b58</button><button type='button' onclick=\"this.form.style.display='none';this.form.previousElementSibling.style.display='inline'\">\u53d6\u6d88</button></form></span>")
                            .append("<span>").append(escapeHtml(todo.getRegisteredDate())).append("</span><span>").append(escapeHtml(todo.getStartDate())).append("</span><span>").append(escapeHtml(todo.getDeadline())).append("</span></li>");
                    }
                    html.append("</ul>");
                    html.append("<div id='deleteConfirm' style='display:none;position:fixed;inset:0;background:rgba(0,0,0,.45);align-items:center;justify-content:center'>")
                        .append("<div style='background:white;padding:24px;border-radius:8px;box-shadow:0 4px 16px #555;text-align:center'>")
                        .append("<p><span id='deleteCount'>0</span>\u4ef6\u3092\u524a\u9664\u3057\u307e\u3059\u3002\u3088\u308d\u3057\u3044\u3067\u3059\u304b\uff1f</p>")
                        .append("<button type='button' onclick=\"document.getElementById('deleteForm').submit()\">\u524a\u9664</button> ")
                        .append("<button type='button' onclick=\"document.getElementById('deleteConfirm').style.display='none';document.querySelectorAll('.delete-selection').forEach(function(b){b.checked=false;b.style.display='none'});var button=document.getElementById('deleteModeButton');button.dataset.active='false';button.textContent='\\u524a\\u9664'\">\u30ad\u30e3\u30f3\u30bb\u30eb</button></div></div>");
                    message = html.toString();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                } else { // ★ SQLite????
                    message = "\u30da\u30fc\u30b8\u304c\u898b\u3064\u304b\u308a\u307e\u305b\u3093"; // ★ SQLite????
                }
                byte[] body = message.getBytes(StandardCharsets.UTF_8); // ★ SQLite????
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (SQLException e) { // ★ SQLite????
                byte[] body = "Database error".getBytes(StandardCharsets.UTF_8); // ★ SQLite????
                exchange.sendResponseHeaders(500, body.length); // ★ SQLite????
                exchange.getResponseBody().write(body); // ★ SQLite????
                e.printStackTrace(); // ★ SQLite????
            } finally { // ★ SQLite????
                exchange.close(); // ★ SQLite????
            }
        });
        server.start();
        System.out.println("\u30b5\u30fc\u30d0\u30fc\u8d77\u52d5 http://localhost:8080 (Ctrl+C\u3067\u7d42\u4e86)");
    }

    private static String listUrl(String query) {
        String titleSearch = URLDecoder.decode(queryValue(query, "titleSearch"), StandardCharsets.UTF_8);
        String deadlineSearch = URLDecoder.decode(queryValue(query, "deadlineSearch"), StandardCharsets.UTF_8);
        return "/?filter=" + requestedFilter(query) + "&sort=" + requestedSort(query) + searchQuery(titleSearch, deadlineSearch);
    }

    private static String formValues(String title, String deadline) {
        return "&todo=" + URLEncoder.encode(title, StandardCharsets.UTF_8)
                + "&deadline=" + URLEncoder.encode(deadline, StandardCharsets.UTF_8);
    }

    private static String searchQuery(String titleSearch, String deadlineSearch) { // ? Encode search parameters for URLs
        return "&titleSearch=" + URLEncoder.encode(titleSearch, StandardCharsets.UTF_8)
                + "&deadlineSearch=" + URLEncoder.encode(deadlineSearch, StandardCharsets.UTF_8);
    }

    private static String requestedFilter(String query) { // ★ Validate selected filter
        String filter = queryValue(query, "filter"); // ★ Read filter query parameter
        if ("active".equals(filter)) return "todo";
        if (filter.equals("todo") || filter.equals("doing") || filter.equals("done")) return filter;
        return "all"; // ★ Default to all tasks
    }

    private static String requestedSort(String query) { // ★ Validate selected sort order
        return "asc".equals(queryValue(query, "sort")) ? "asc" : "desc"; // ★ Default to newest first
    }

    private static String queryValue(String query, String name) { // ★ Read one URL query parameter
        if (query == null) return ""; // ★ Handle missing query
        for (String parameter : query.split("&")) { // ★ Check each query parameter
            if (parameter.startsWith(name + "=")) return parameter.substring(name.length() + 1); // ★ Return matching value
        }
        return ""; // ★ Return empty when parameter is absent
    }

    private static int requestedId(String query) { // ★ Parse Todo ID
        String value = queryValue(query, "id"); // ★ Parse Todo ID
        try { // ★ Parse Todo ID
            return Integer.parseInt(value); // ★ Parse Todo ID
        } catch (NumberFormatException e) { // ★ Parse Todo ID
            return -1; // ★ Parse Todo ID
        }
    }

    private static String escapeHtml(String value) { // ★ SQLite????
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") // ★ SQLite????
                .replace("\"", "&quot;").replace("'", "&#39;"); // ★ SQLite????
    }
}

class Todo { // ★ Todo deadline field
    private final int id; // ★ Todo deadline field
    private final String title; // ★ Todo deadline field
    private final boolean done; // ★ Todo deadline field
    private final String registeredDate;
    private final String startDate;
    private final String deadline; // ★ Todo deadline field

    Todo(int id, String title, boolean done, String registeredDate, String startDate, String deadline) { // ★ Todo deadline field
        this.id = id; // ★ Todo deadline field
        this.title = title; // ★ Todo deadline field
        this.done = done; // ★ Todo deadline field
        this.registeredDate = registeredDate;
        this.startDate = startDate;
        this.deadline = deadline; // ★ Todo deadline field
    }

    int getId() { return id; } // ★ Todo deadline field
    String getTitle() { return title; } // ★ Todo deadline field
    boolean isDone() { return done; } // ★ Todo deadline field
    String getRegisteredDate() { return registeredDate; }
    String getStartDate() { return startDate; }
    String getDeadline() { return deadline; } // ★ Todo deadline field
}
