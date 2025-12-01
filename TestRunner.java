import com.http.server.HttpServer;
import com.http.client.HttpClient;
import com.http.common.HttpResponse;
import com.http.common.HttpRequest;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== 开始自动化测试 ===");
        
        // 1. 启动服务器
        int port = 6175;
        HttpServer server = new HttpServer(port);
        Thread serverThread = new Thread(() -> {
            server.start();
        });
        serverThread.start();
        System.out.println("服务器线程已启动");

        try {
            // 等待服务器启动
            Thread.sleep(1000);

            // 2. 使用 HttpClient 发送请求
            System.out.println("正在发送 GET 请求...");
            HttpClient client = new HttpClient("localhost", port);
            
            // 测试 1: 访问根路径 (静态文件)
            HttpResponse response = client.get("/");
            System.out.println("Response Status: " + response.getStatusCode());
            if (response.getStatusCode() == 200) {
                System.out.println("✅ 静态文件访问测试通过");
            } else {
                System.out.println("❌ 静态文件访问测试失败: " + response.getStatusCode());
            }

            // 测试 2: 测试重定向
            System.out.println("正在发送重定向测试请求...");
            HttpResponse redirectResponse = client.get("/old-page");
            System.out.println("Redirect Status: " + redirectResponse.getStatusCode());
            if (redirectResponse.getStatusCode() == 301) {
                System.out.println("✅ 重定向状态码测试通过");
                String location = redirectResponse.getHeader("Location");
                System.out.println("Location: " + location);
            } else {
                System.out.println("❌ 重定向测试失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 3. 停止服务器
            System.out.println("正在停止服务器...");
            server.stop();
            System.out.println("=== 测试结束 ===");
            System.exit(0);
        }
    }
}
