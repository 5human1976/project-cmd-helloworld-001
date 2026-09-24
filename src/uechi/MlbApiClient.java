package uechi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 【ソースファイル名】
 * MlbApiClient.java
 *
 * 【概要】
 * MLB Stats APIから日本時間の当日の日付に対応する試合一覧を取得します。
 * JSONレスポンスを保存し、HTTP通信とファイル出力の動作確認に使用します。
 *
 * 【作成日】2026-09-24
 * 【最終更新日】2026-09-24
 *
 * @author masa
 * @version 1.0
 */
public class MlbApiClient {
    private static final String SCHEDULE_URL =
            "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=";
    private static final Path OUTPUT_FILE = Path.of("output", "games.json");

    /**
     * 当日のMLBの試合一覧を取得し、JSONファイルに保存します。
     * 失敗時は原因を表示し、終了コード1で終了します。
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        // 日本時間の当日の日付を取得し、APIの検索対象日として表示します。
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        System.out.println("MLB APIへアクセスします。");
        System.out.println("対象日 : " + date);
        System.out.println();

        // 試合一覧を取得し、通信エラーと割り込みをそれぞれ処理します。
        String json;
        try {
            json = fetchGames(date);
        } catch (InterruptedException e) {
            // 割り込み状態を復元し、中断されたことを表示して終了します。
            Thread.currentThread().interrupt();
            System.err.println("API通信が中断されました: " + e);
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("API取得に失敗しました: " + e);
            System.exit(1);
            return;
        }

        // 取得したJSONを保存し、保存先またはファイル出力の失敗原因を表示します。
        try {
            saveJson(json);
            System.out.println();
            System.out.println("出力ファイル:");
            System.out.println("output/games.json");
            System.out.println("絶対パス: " + OUTPUT_FILE.toAbsolutePath());
            System.out.println();
            System.out.println("処理が正常終了しました。");
        } catch (IOException | SecurityException e) {
            System.err.println("ファイル出力に失敗しました: "
                    + OUTPUT_FILE.toAbsolutePath() + " : " + e);
            System.exit(1);
        }
    }

    /**
     * 指定した日付のMLBの試合一覧をJSON文字列で取得します。
     *
     * @param date APIで検索する試合日
     * @return APIが返した未加工のJSON文字列
     * @throws IOException 通信に失敗した場合、またはHTTPステータスが200以外の場合
     * @throws InterruptedException 通信中にスレッドが割り込まれた場合
     */
    private static String fetchGames(LocalDate date) throws IOException, InterruptedException {
        // 接続待ち時間を10秒に設定したHTTPクライアントを作成します。
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // MLBを指定するsportId=1と対象日を含むGETリクエストを作成します。
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SCHEDULE_URL + date))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        // APIを同期呼び出しし、レスポンス本文をUTF-8の文字列として取得します。
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // HTTPステータスを確認し、200以外の場合は保存せずに例外を通知します。
        int statusCode = response.statusCode();
        System.out.println("HTTP Status : " + statusCode);
        if (statusCode != 200) {
            throw new IOException("HTTPステータスが200ではありません: " + statusCode);
        }

        // 正常なレスポンスの本文を、JSON解析や整形をせずに返却します。
        System.out.println("API取得成功");
        return response.body();
    }

    /**
     * JSON文字列をoutput/games.jsonにUTF-8で保存します。
     *
     * @param json 保存するJSON文字列
     * @throws IOException フォルダの作成またはファイルへの書き込みに失敗した場合
     */
    private static void saveJson(String json) throws IOException {
        // 出力フォルダが存在しない場合は作成します。
        Files.createDirectories(OUTPUT_FILE.getParent());

        // JSONをUTF-8で書き込み、既存ファイルがある場合は上書きします。
        Files.writeString(OUTPUT_FILE, json, StandardCharsets.UTF_8);
    }
}
