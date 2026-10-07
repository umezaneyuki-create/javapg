import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class App {
    private static Connection db; // ★ SQLite????

    private static void initializeDatabase() throws SQLException { // ★ Add and migrate deadline column
        db = DriverManager.getConnection("jdbc:sqlite:todos.db"); // ★ Add and migrate deadline column
        try (PreparedStatement s = db.prepareStatement("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER, deadline TEXT, registered_date TEXT, start_date TEXT, deleted INTEGER DEFAULT 0, parent_id INTEGER)")) { // ★ Add and migrate deadline column
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
        boolean hasParentId = false;
        try (PreparedStatement s = db.prepareStatement("PRAGMA table_info(todos)"); ResultSet rs = s.executeQuery()) {
            while (rs.next()) if ("parent_id".equals(rs.getString("name"))) hasParentId = true;
        }
        if (!hasParentId) {
            try (PreparedStatement s = db.prepareStatement("ALTER TABLE todos ADD COLUMN parent_id INTEGER")) { s.executeUpdate(); }
        }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deleted = 0 WHERE deleted IS NULL")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET registered_date = date('now','localtime') WHERE registered_date IS NULL OR registered_date = ''")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET start_date = '' WHERE start_date IS NULL")) { s.executeUpdate(); }
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deadline = '10/13' WHERE deadline IS NULL OR deadline = ''")) { // ★ Add and migrate deadline column
            s.executeUpdate(); // ★ Add and migrate deadline column
        }
    }

    private static synchronized void addTodo(String title, String startDate, String deadline) throws SQLException { // ★ SQLite????
        try (PreparedStatement s = db.prepareStatement("INSERT INTO todos (title, done, registered_date, start_date, deadline, parent_id) VALUES (?, 0, date('now','localtime'), ?, ?, NULL)")) { // ★ SQLite????
            s.setString(1, title); // ★ SQLite????
            s.setString(2, startDate);
            s.setString(3, deadline);
            s.executeUpdate(); // ★ SQLite????
        }
    }

    private static synchronized boolean addSubtask(String title, String deadline, int parentId) throws SQLException {
        try (PreparedStatement s = db.prepareStatement("INSERT INTO todos (title, done, registered_date, start_date, deadline, deleted, parent_id) SELECT ?, 0, date('now','localtime'), '', ?, 0, id FROM todos WHERE id = ? AND deleted = 0 AND parent_id IS NULL")) {
            s.setString(1, title);
            s.setString(2, deadline);
            s.setInt(3, parentId);
            return s.executeUpdate() == 1;
        }
    }

    private static synchronized void markDone(int id) throws SQLException { // ★ SQLite????
        try (PreparedStatement s = db.prepareStatement("UPDATE todos SET done = CASE WHEN done = 0 THEN 1 ELSE 0 END WHERE id = ? AND deleted = 0")) { // ★ SQLite????
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
        try (PreparedStatement s = db.prepareStatement("SELECT id, title, done, registered_date, start_date, deadline, parent_id FROM todos WHERE id = ? AND deleted = 0")) { // ★ Edit Todo title
            s.setInt(1, id); // ★ Edit Todo title
            try (ResultSet rs = s.executeQuery()) { // ★ Edit Todo title
                if (rs.next()) return new Todo(rs.getInt("id"), rs.getString("title"), rs.getInt("done") != 0, rs.getString("registered_date"), rs.getString("start_date"), rs.getString("deadline"), rs.getObject("parent_id") == null ? null : rs.getInt("parent_id")); // ★ Edit Todo title
            }
        }
        return null; // ★ Edit Todo title
    }

    private static synchronized void deleteTodo(int id) throws SQLException { // ★ SQLite????
        boolean parent;
        try (PreparedStatement s = db.prepareStatement("SELECT parent_id FROM todos WHERE id = ? AND deleted = 0")) {
            s.setInt(1, id);
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) return;
                parent = rs.getObject("parent_id") == null;
            }
        }
        String sql = parent
                ? "UPDATE todos SET deleted = 1 WHERE id = ? OR parent_id = ?"
                : "UPDATE todos SET deleted = 1 WHERE id = ?";
        try (PreparedStatement s = db.prepareStatement(sql)) { // ★ SQLite????
            s.setInt(1, id); // ★ SQLite????
            if (parent) s.setInt(2, id);
            s.executeUpdate(); // ★ SQLite????
        }
    }

    private static synchronized void deleteVisibleCompleted(String titleSearch, String deadlineSearch) throws SQLException { // ★ Delete completed tasks currently visible
        StringBuilder targets = new StringBuilder("SELECT id, parent_id FROM todos WHERE done = 1 AND deleted = 0");
        if (!titleSearch.trim().isEmpty()) targets.append(" AND title LIKE ?");
        if (!deadlineSearch.trim().isEmpty()) targets.append(" AND deadline LIKE ?");
        Set<Integer> parentIds = new HashSet<>();
        String targetSql = targets.toString();
        try (PreparedStatement s = db.prepareStatement(targetSql)) {
            int parameter = 1;
            if (!titleSearch.trim().isEmpty()) s.setString(parameter++, "%" + titleSearch + "%");
            if (!deadlineSearch.trim().isEmpty()) s.setString(parameter++, "%" + deadlineSearch + "%");
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    if (rs.getObject("parent_id") == null) parentIds.add(rs.getInt("id"));
                }
            }
        }
        for (int parentId : parentIds) {
            try (PreparedStatement s = db.prepareStatement("UPDATE todos SET deleted = 1 WHERE parent_id = ?")) {
                s.setInt(1, parentId);
                s.executeUpdate();
            }
        }
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
        String orderBy = "all".equals(filter)
                ? " ORDER BY CASE WHEN done = 0 AND (start_date IS NULL OR TRIM(start_date) = '') THEN 0 WHEN done = 0 AND start_date IS NOT NULL AND TRIM(start_date) <> '' THEN 1 ELSE 2 END, id " + direction
                : " ORDER BY id " + direction;
        String sql = "SELECT id, title, done, registered_date, start_date, deadline, parent_id FROM todos" + condition + orderBy; // ? Search within selected tab
        try (PreparedStatement s = db.prepareStatement(sql)) { // ★ Search within selected tab
            int parameter = 1; // ★ Search within selected tab
            if (!titleSearch.trim().isEmpty()) s.setString(parameter++, "%" + titleSearch + "%"); // ★ Search within selected tab
            if (!deadlineSearch.trim().isEmpty()) s.setString(parameter++, "%" + deadlineSearch + "%"); // ★ Search within selected tab
            try (ResultSet rs = s.executeQuery()) { // ★ Search within selected tab
                while (rs.next()) { // ★ Search within selected tab
                    todos.add(new Todo(rs.getInt("id"), rs.getString("title"), rs.getInt("done") != 0, rs.getString("registered_date"), rs.getString("start_date"), rs.getString("deadline"), rs.getObject("parent_id") == null ? null : rs.getInt("parent_id"))); // ★ Search within selected tab
                }
            }
        }
        return todos; // ★ Search within selected tab
    }

    private static List<Todo> organizeTodos(List<Todo> todos) {
        List<Todo> ordered = new ArrayList<>();
        for (Todo parent : todos) {
            if (parent.getParentId() != null) continue;
            ordered.add(parent);
            for (Todo candidate : todos) {
                if (parent.getId() == (candidate.getParentId() == null ? -1 : candidate.getParentId())) ordered.add(candidate);
            }
        }
        for (Todo todo : todos) if (!ordered.contains(todo)) ordered.add(todo);
        return ordered;
    }

    private static List<Todo> loadTodosForDisplay(String filter, String sort, String titleSearch, String deadlineSearch,
                                                   Set<Integer> contextOnlyIds) throws SQLException {
        List<Todo> matches = loadTodos(filter, sort, titleSearch, deadlineSearch);
        if ("all".equals(filter) && titleSearch.trim().isEmpty() && deadlineSearch.trim().isEmpty()) {
            return organizeTodos(matches);
        }

        List<Todo> display = new ArrayList<>(matches);
        Set<Integer> matchingIds = new HashSet<>();
        for (Todo todo : matches) matchingIds.add(todo.getId());
        Set<Integer> neededParents = new HashSet<>();
        for (Todo todo : matches) {
            if (todo.getParentId() != null && !matchingIds.contains(todo.getParentId())) neededParents.add(todo.getParentId());
        }
        for (int parentId : neededParents) {
            Todo parent = findTodo(parentId);
            if (parent != null) {
                display.add(parent);
                contextOnlyIds.add(parentId);
            }
        }
        Comparator<Todo> byId = Comparator.comparingInt(Todo::getId);
        if ("desc".equals(sort)) byId = byId.reversed();
        if ("all".equals(filter)) {
            Comparator<Todo> byCategory = Comparator.comparingInt(todo -> todo.isDone() ? 2
                    : (todo.getStartDate() == null || todo.getStartDate().trim().isEmpty() ? 0 : 1));
            display.sort(byCategory.thenComparing(byId));
        } else {
            display.sort(byId);
        }
        return organizeTodos(display);
    }

    private static synchronized int[] loadTabCounts() throws SQLException {
        int all = 0;
        int todo = 0;
        int doing = 0;
        int done = 0;
        try (PreparedStatement s = db.prepareStatement("SELECT done, start_date FROM todos WHERE deleted = 0");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                all++;
                boolean isDone = rs.getInt("done") != 0;
                String startDate = rs.getString("start_date");
                if (isDone) {
                    done++;
                } else if (startDate == null || startDate.trim().isEmpty()) {
                    todo++;
                } else {
                    doing++;
                }
            }
        }
        return new int[] { all, todo, doing, done };
    }

    public static void main(String[] args) throws Exception {
        initializeDatabase(); // ★ SQLite????
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String message;
            if ("/todo.css".equals(path) || "/todo.js".equals(path)
                    || path.startsWith("/static/assets/")) {
                String assetName = path.substring(path.lastIndexOf('/') + 1);
                if (path.startsWith("/static/assets/") && !Set.of(
                        "cat-header.png", "cat-pencil-cup.png", "cat-sidebar.png", "paw-duo.png", "paw.png", "done-stamp.png", "pink-tape.png", "ribbon.png",
                        "title-paper-banner.png", "speech-bubble-large-paw.png", "speech-bubble-small-paw.png",
                        "tab-icon-cat-all-white.png", "tab-icon-cat-todo-black.png", "tab-icon-cat-doing-blue.png", "tab-icon-cat-done-cream.png",
                        "fishbone-sticker.png", "notepad-pencil-icon.png", "bar-chart-icon.png").contains(assetName)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                Path asset = path.startsWith("/static/assets/")
                        ? Path.of("static", "assets", assetName)
                        : Path.of("static", path.substring(1));
                byte[] assetBody = Files.readAllBytes(asset);
                exchange.getResponseHeaders().set("Content-Type", path.endsWith(".css") ? "text/css; charset=UTF-8"
                        : path.endsWith(".js") ? "application/javascript; charset=UTF-8" : "image/png");
                exchange.sendResponseHeaders(200, assetBody.length);
                exchange.getResponseBody().write(assetBody);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            try { // ★ SQLite????
                if (path.equals("/add-subtask") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String title = URLDecoder.decode(queryValue(body, "todo"), StandardCharsets.UTF_8);
                    String deadline = URLDecoder.decode(queryValue(body, "deadline"), StandardCharsets.UTF_8);
                    int parentId = requestedParentId(body);
                    String error = "";
                    if (title.trim().isEmpty()) error = "todo";
                    else if (deadline.trim().isEmpty()) error = "deadline";
                    else if (parentId <= 0 || !addSubtask(title, deadline, parentId)) error = "parent";
                    if (error.isEmpty()) exchange.getResponseHeaders().set("Location", "/");
                    else exchange.getResponseHeaders().set("Location", "/?error=" + error + formValues(title, deadline) + "&parentId=" + parentId);
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                } else if (path.equals("/add") && method.equals("POST")) {
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
                    Set<Integer> contextOnlyIds = new HashSet<>();
                    List<Todo> todos = loadTodosForDisplay(filter, sort, titleSearch, deadlineSearch, contextOnlyIds);
                    int[] tabCounts = loadTabCounts();
                    StringBuilder html = new StringBuilder(
                            "<!doctype html><html lang='ja'><head><meta charset='UTF-8'>"
                            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                            + "<title>○○さんのやることリスト</title>"
                            + "<link rel='preconnect' href='https://fonts.googleapis.com'>"
                            + "<link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>"
                            + "<link href='https://fonts.googleapis.com/css2?family=Hachi+Maru+Pop&family=Noto+Sans+JP:wght@400;500;600;700&display=swap' rel='stylesheet'>"
                            + "<link rel='stylesheet' href='/todo.css'></head><body>"
                            + "<div class='page-frame'>"
                            + "<header class='hero-header'>"
                            + "<div class='hero-brand'><img class='brand-cat-mark-image' src='/static/assets/tab-icon-cat-all-white.png' alt=''><div><p class='brand-title'>ねこと、はたらく。</p><p class='brand-copy'>かわいいToDoで<br>今日もいい日に…♪</p></div></div>"
                            + "<div class='hero-title-wrap'><img class='title-paper-image' src='/static/assets/title-paper-banner.png' alt=''><img class='title-ribbon' src='/static/assets/ribbon.png' alt=''><h1>○○さんの<br><span>やることリスト</span></h1></div>"
                            + "<div class='hero-message'><img src='/static/assets/speech-bubble-large-paw.png' alt=''><p>やることを片づけて<br>すきなことでいっぱいの<br>毎日にしよう…♡</p></div>"
                            + "<img class='hero-cat' src='/static/assets/cat-header.png' alt='本の上で眠る三毛猫'>"
                            + "</header>"
                            + "<div class='workspace'>"
                            + "<aside class='sidebar'>"
                            + "<nav class='side-nav'>"
                            + "<span class='nav-item inactive'><span class='nav-icon'>⌂</span><span>ホーム</span></span>"
                            + "<span class='nav-item active'><span class='nav-icon nav-paw'><img src='/static/assets/paw.png' alt=''></span><span>やることリスト</span></span>"
                            + "<span class='nav-item inactive'><span class='nav-icon'>▦</span><span>カレンダー</span></span>"
                            + "<span class='nav-item inactive'><span class='nav-icon'><img class='nav-asset' src='/static/assets/notepad-pencil-icon.png' alt=''></span><span>ノート</span></span>"
                            + "<span class='nav-item inactive'><span class='nav-icon'><img class='nav-asset' src='/static/assets/bar-chart-icon.png' alt=''></span><span>統計・ふりかえり</span></span>"
                            + "<span class='nav-item inactive'><span class='nav-icon'>⚙</span><span>設定</span></span>"
                            + "</nav>"
                            + "<div class='sidebar-note'><img src='/static/assets/paw-duo.png' alt=''><p>コツコツやれば<br>きっとできるよ<br>にゃ…♡</p></div>"
                            + "<img class='sidebar-cat' src='/static/assets/cat-sidebar.png' alt='座っている三毛猫'>"
                            + "</aside>"
                            + "<main class='main-content'>"
                            + "<section class='toolbar-card'>"
                            + "<div class='toolbar-decoration'><img class='toolbar-cat' src='/static/assets/cat-pencil-cup.png' alt='ペン立てから顔を出す猫'><div class='toolbar-bubble'><img src='/static/assets/speech-bubble-small-paw.png' alt=''><span>やることを<br>登録しよう…！</span></div><img class='toolbar-fish' src='/static/assets/fishbone-sticker.png' alt=''></div>");
                    String error = queryValue(query, "error");
                    String formTitle = error.isEmpty() ? titleSearch : URLDecoder.decode(queryValue(query, "todo"), StandardCharsets.UTF_8);
                    String formDeadline = error.isEmpty() ? deadlineSearch : URLDecoder.decode(queryValue(query, "deadline"), StandardCharsets.UTF_8);
                    if ("todo".equals(error)) html.append("<p class='form-error'>「やること」を入力してください</p>");
                    if ("deadline".equals(error)) html.append("<p class='form-error'>「締切日」を入力してください</p>");
                    if ("parent".equals(error)) html.append("<p class='form-error'>親タスクを選択してください</p>");
                    html.append("<form class='todo-toolbar' method='post' action='/add' novalidate><input type='hidden' id='selectedTaskId'><input type='hidden' id='selectedParentId' name='parentId' value=''>")
                        .append("<div class='input-row'>")
                        .append("<label class='title-field'><span>内容</span><input name='todo' placeholder='やることを入力してください…　例）企画書を作成する' value='").append(escapeHtml(formTitle)).append("'></label>")
                        .append("<label class='deadline-field'><span>締切日</span><input name='deadline' placeholder='10/13' value='").append(escapeHtml(formDeadline)).append("'></label>")
                        .append("</div>")
                        .append("<div class='action-buttons'>")
                        .append("<button class='btn btn-save'><span class='button-symbol'>＋</span>登録</button>")
                        .append("<button class='btn btn-subtask' type='submit' id='subtaskButton' formaction='/add-subtask' disabled><span class='button-symbol'>＋</span>サブタスク登録</button>")
                        .append("<button class='btn btn-search' type='button' id='searchButton'><span class='button-symbol'>⌕</span>検索</button>")
                        .append("<button class='btn btn-edit' type='button' id='editButton'><span class='button-symbol'>✎</span>編集</button>")
                        .append("<button class='btn btn-delete' type='button' id='deleteButton'><span class='button-symbol'>⌫</span>削除</button>")
                        .append("</div>")
                        .append("<div id='selectedTask' class='selected-task'><img src='/static/assets/paw.png' alt=''><span>選択中：なし</span></div>")
                        .append("</form></section>");
                    html.append("<div class='tabs-row'><nav class='todo-tabs'>")
                        .append("<a class='tab tab-all").append("all".equals(filter) ? " active" : "").append("' href='/?filter=all&amp;sort=desc'><img src='/static/assets/tab-icon-cat-all-white.png' alt=''><span>全部 <small>(").append(tabCounts[0]).append(")</small></span></a>")
                        .append("<a class='tab tab-todo").append("todo".equals(filter) ? " active" : "").append("' href='/?filter=todo&amp;sort=desc'><img src='/static/assets/tab-icon-cat-todo-black.png' alt=''><span>やること <small>(").append(tabCounts[1]).append(")</small></span></a>")
                        .append("<a class='tab tab-doing").append("doing".equals(filter) ? " active" : "").append("' href='/?filter=doing&amp;sort=desc'><img src='/static/assets/tab-icon-cat-doing-blue.png' alt=''><span>やってること <small>(").append(tabCounts[2]).append(")</small></span></a>")
                        .append("<a class='tab tab-done").append("done".equals(filter) ? " active" : "").append("' href='/?filter=done&amp;sort=desc'><img src='/static/assets/tab-icon-cat-done-cream.png' alt=''><span>やったこと <small>(").append(tabCounts[3]).append(")</small></span></a>")
                        .append("</nav><div class='tabs-tip'><img src='/static/assets/speech-bubble-small-paw.png' alt=''><span>タブで絞り込み表示<br>できるよ！</span></div></div>")
                        .append("<section class='list-card'>");
                    if ("desc".equals(sort)) {
                        html.append("<p class='sort-control'><a href='/?filter=").append(filter).append("&amp;sort=asc").append(htmlSearchParams).append("'>古い順</a></p>");
                    } else {
                        html.append("<p class='sort-control'><a href='/?filter=").append(filter).append("&amp;sort=desc").append(htmlSearchParams).append("'>新しい順</a></p>");
                    }
                    html.append("<form id='deleteForm' method='post' action='/delete-selected'>")
                        .append("<input type='hidden' name='deleteIds' id='deleteTaskId'><input type='hidden' name='filter' value='").append(filter).append("'><input type='hidden' name='sort' value='").append(sort).append("'><input type='hidden' name='titleSearch' value='").append(escapeHtml(titleSearch)).append("'><input type='hidden' name='deadlineSearch' value='").append(escapeHtml(deadlineSearch)).append("'></form>")
                        .append("<div class='notebook-holes' aria-hidden='true'></div>")
                        .append("<div class='todo-table'><div class='todo-header'>")
                        .append("<span>☑</span><span>内容</span><span>登録日</span><span>開始日</span><span>締切日</span></div>")
                        .append("<ul class='todo-list'>");
                    for (Todo todo : todos) {
                        boolean contextOnly = contextOnlyIds.contains(todo.getId());
                        String statusClass = todo.isDone() ? "status-done" : (todo.getStartDate() == null || todo.getStartDate().trim().isEmpty() ? "status-todo" : "status-doing");
                        html.append("<li class='todo-row ").append(statusClass).append(" ").append(todo.getParentId() == null ? "parent-row" : "subtask-row").append(contextOnly ? " context-only" : " selectable-row").append("' data-id='").append(todo.getId()).append("' data-parent='").append(todo.getParentId() == null).append("' data-title='").append(escapeHtml(todo.getTitle())).append("' data-start='").append(escapeHtml(todo.getStartDate())).append("' data-deadline='").append(escapeHtml(todo.getDeadline())).append("'>")
                            .append("<span class='check-cell'>");
                        if (!contextOnly) {
                            html.append("<a class='completion-toggle' href='/done?id=").append(todo.getId()).append("&amp;filter=").append(filter).append("&amp;sort=").append(sort).append(htmlSearchParams).append("'>").append(todo.isDone() ? "☑" : "☐").append("</a>");
                        }
                        html.append("</span><span class='task-content'>");
                        html.append("<span class='task-title'>").append(escapeHtml(todo.getTitle())).append("</span>");
                        if (todo.isDone()) html.append("<img class='done-stamp' src='/static/assets/done-stamp.png' alt='済'>");
                        html.append("</span>")
                            .append("<span class='date-cell'>").append(escapeHtml(todo.getRegisteredDate())).append("</span>")
                            .append("<span class='date-cell'>").append(escapeHtml(todo.getStartDate())).append("</span>")
                            .append("<span class='date-cell deadline-cell'>").append(escapeHtml(todo.getDeadline())).append("</span></li>");
                    }
                    html.append("</ul></div>");
                    html.append("<form id='editForm' method='post' action='/edit' class='hidden'><input type='hidden' name='id' id='editId'><input type='hidden' name='filter' value='").append(filter).append("'><input type='hidden' name='sort' value='").append(sort).append("'><input type='hidden' name='titleSearch' value='").append(escapeHtml(titleSearch)).append("'><input type='hidden' name='deadlineSearch' value='").append(escapeHtml(deadlineSearch)).append("'></form>");
                    html.append("</section>");
                    html.append("<div id='editDialog' class='modal-overlay hidden'><div class='modal-card'><img class='modal-paw' src='/static/assets/paw.png' alt=''><h2>タスクを編集</h2><label>内容<input id='editTitle' name='todo' form='editForm' required></label><label>開始日<input id='editStart' name='startDate' form='editForm'></label><label>締切日<input id='editDeadline' name='deadline' form='editForm' required></label><div class='modal-actions'><button class='btn btn-neutral' type='button' data-close='editDialog'>キャンセル</button><button class='btn btn-save' type='submit' form='editForm'>保存</button></div></div></div>");
                    html.append("<div id='deleteConfirm' class='modal-overlay hidden'><div class='modal-card compact-card'><img class='modal-paw' src='/static/assets/paw.png' alt=''><p id='deleteMessage'>削除対象です。</p><div class='modal-actions'><button class='btn btn-neutral' type='button' data-close='deleteConfirm'>キャンセル</button><button class='btn btn-delete' type='submit' form='deleteForm'>削除</button></div></div></div>");
                    html.append("<script src='/todo.js' defer></script></main></div></div></body></html>");
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

    private static int requestedParentId(String query) {
        try { return Integer.parseInt(queryValue(query, "parentId")); }
        catch (NumberFormatException e) { return -1; }
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
    private final Integer parentId;

    Todo(int id, String title, boolean done, String registeredDate, String startDate, String deadline, Integer parentId) { // ★ Todo deadline field
        this.id = id; // ★ Todo deadline field
        this.title = title; // ★ Todo deadline field
        this.done = done; // ★ Todo deadline field
        this.registeredDate = registeredDate;
        this.startDate = startDate;
        this.deadline = deadline; // ★ Todo deadline field
        this.parentId = parentId;
    }

    int getId() { return id; } // ★ Todo deadline field
    String getTitle() { return title; } // ★ Todo deadline field
    boolean isDone() { return done; } // ★ Todo deadline field
    String getRegisteredDate() { return registeredDate; }
    String getStartDate() { return startDate; }
    String getDeadline() { return deadline; } // ★ Todo deadline field
    Integer getParentId() { return parentId; }
}
