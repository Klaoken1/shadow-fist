import UIKit
import WebKit

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        application.isIdleTimerDisabled = true
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.rootViewController = GameViewController()
        window.makeKeyAndVisible()
        self.window = window
        return true
    }
}

final class GameViewController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate {
    private var web: WKWebView!

    override func loadView() {
        let config = WKWebViewConfiguration()
        // Same idea as the Android AssetClient: a fixed origin gives localStorage (the save game) a stable home.
        config.setURLSchemeHandler(AssetHandler(), forURLScheme: AssetHandler.scheme)
        config.mediaTypesRequiringUserActionForPlayback = []
        config.allowsInlineMediaPlayback = true
        config.userContentController.add(self, name: "haptic")
        web = WKWebView(frame: .zero, configuration: config)
        web.isOpaque = false
        web.backgroundColor = UIColor(red: 0.043, green: 0.043, blue: 0.059, alpha: 1)
        web.scrollView.isScrollEnabled = false
        web.scrollView.bounces = false
        web.scrollView.contentInsetAdjustmentBehavior = .never
        web.navigationDelegate = self
        view = web
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        web.load(URLRequest(url: URL(string: "\(AssetHandler.scheme)://localhost/index.html")!))
        NotificationCenter.default.addObserver(self, selector: #selector(pauseGame),
                                               name: UIApplication.willResignActiveNotification, object: nil)
    }

    @objc private func pauseGame() {
        web.evaluateJavaScript("window.pauseGame && pauseGame(); 0", completionHandler: nil)
    }

    // iOS can kill the page's process under memory pressure; progress is already saved, so just reload.
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        webView.reload()
    }

    // vib(ms) in the game: long buzzes (getting hit) are heavy, short ones (kills) are light.
    func userContentController(_ controller: WKUserContentController, didReceive message: WKScriptMessage) {
        let ms = message.body as? Int ?? 10
        UIImpactFeedbackGenerator(style: ms >= 50 ? .heavy : .light).impactOccurred()
    }

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .landscape }
    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    // Fast tapping near the screen edges shouldn't trigger the system swipes.
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge { .all }
}

/// Serves the bundled game files from the "assets" folder. Nothing leaves the device.
final class AssetHandler: NSObject, WKURLSchemeHandler {
    static let scheme = "app"

    func webView(_ webView: WKWebView, start task: WKURLSchemeTask) {
        guard let url = task.request.url, let root = Bundle.main.resourceURL else { return }
        let file = root.appendingPathComponent("assets").appendingPathComponent(url.path)
        let data = try? Data(contentsOf: file)
        let type = url.pathExtension == "html" ? "text/html; charset=utf-8" : "application/octet-stream"
        let response = HTTPURLResponse(url: url, statusCode: data == nil ? 404 : 200, httpVersion: "HTTP/1.1",
                                       headerFields: ["Content-Type": type])!
        task.didReceive(response)
        task.didReceive(data ?? Data())
        task.didFinish()
    }

    func webView(_ webView: WKWebView, stop task: WKURLSchemeTask) {}
}
