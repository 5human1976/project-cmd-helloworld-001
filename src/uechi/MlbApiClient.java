package uechi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 【ソースファイル名】
 * MlbApiClient.java
 *
 * 【概要】
 * MLB Stats APIの2日分の試合から、日本時間の当日に開始する試合を抽出します。
 * 試合一覧を表示し、必要な情報を整形したJSONとして保存することで日時変換とJSON処理の学習に使用します。
 *
 * 【作成日】2026-09-24
 * 【最終更新日】2026-10-05
 *
 * @author masa
 * @version 1.0
 */
public class MlbApiClient {
    private static final String SCHEDULE_URL =
            "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=";

    /**
     * 日本時間の当日に開始するMLBの試合一覧を表示し、整形したJSONファイルに保存します。
     * 失敗時は原因を表示し、終了コード1で終了します。
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        // 開始時点の経過時間計測用の値をナノ秒単位で記録します。
        long startTime = System.nanoTime();

        // 日本時間の当日の日付を取得し、日付を含む出力先を作成します。
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        Path outputFile = Path.of("output",
                "games_" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + ".json");

        // 見出しと対象日を表示し、終了コードと通信エラー用のメッセージを準備します。
        System.out.println("=== MLB 試合情報取得 ===");
        System.out.println();
        System.out.println("対象日       : " + date);
        int exitCode = 0;
        String httpStatus = "-";
        String errorMessage = "MLB APIへの接続に失敗しました。";
        int totalGames = -1;

        // JSON解析用のObjectMapperと、採用した試合・試合IDの保存先を用意します。
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode games = mapper.createArrayNode();
        Set<Long> gamePks = new HashSet<>();

        // 前日と当日の2日分を取得し、両方が成功した場合だけ結果を保存します。
        try {
            for (LocalDate candidateDate : new LocalDate[] { date.minusDays(1), date }) {
                // 各通信の前にステータスを未取得に戻し、候補日のレスポンスを取得します。
                httpStatus = "-";
                errorMessage = "MLB APIへの接続に失敗しました。";
                HttpResponse<String> response = fetchGames(candidateDate);
                httpStatus = Integer.toString(response.statusCode());
                if (response.statusCode() != 200) {
                    // HTTPエラーの場合は、残りの処理と保存を行わず異常終了にします。
                    errorMessage = "MLB APIから正常なレスポンスを取得できませんでした。";
                    exitCode = 1;
                    break;
                }

                // JSONを解析し、日本時間の対象日に一致する試合だけを追加します。
                errorMessage = "MLB APIのレスポンスから試合情報を取得できませんでした。";
                collectGames(response.body(), date, mapper, games, gamePks);
            }

            // 両方の取得が成功した場合は、必要な項目で出力用JSONを作成します。
            if (exitCode == 0) {
                errorMessage = "試合情報のJSON作成に失敗しました。";
                ObjectNode output = mapper.createObjectNode();
                output.put("targetDate", date.toString());
                output.put("timeZone", "Asia/Tokyo");
                output.put("totalGames", games.size());
                output.set("games", games);
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
                totalGames = games.size();

                // 整形したJSONを日付付きのファイルへUTF-8で保存します。
                errorMessage = "ファイルの出力に失敗しました。";
                saveJson(json, outputFile);
            }
        } catch (InterruptedException e) {
            // 割り込み状態を復元し、通信の中断を記録して調査用の例外情報を表示します。
            Thread.currentThread().interrupt();
            errorMessage = "MLB APIへの通信が中断されました。";
            e.printStackTrace(System.err);
            exitCode = 1;
        } catch (IOException | SecurityException | IllegalArgumentException | DateTimeException e) {
            // 失敗した処理のメッセージを保持し、調査用の例外情報を表示します。
            e.printStackTrace(System.err);
            exitCode = 1;
        } finally {
            // 最後の通信結果と、両日分の抽出が完了した場合の試合数・出力先を表示します。
            System.out.println("HTTP Status  : " + httpStatus);
            if (totalGames >= 0) {
                System.out.println("取得試合数   : " + totalGames);

                // 正常終了の場合は、抽出済みの試合一覧を出力先の前に表示します。
                if (exitCode == 0) {
                    printGames(games);
                }
                System.out.println("出力ファイル : " + outputFile.toString().replace('\\', '/'));
            }

            // 正常終了または異常終了を表示し、失敗時は日本語のエラー内容を表示します。
            System.out.println("処理結果     : " + (exitCode == 0 ? "正常終了" : "異常終了"));
            if (exitCode != 0) {
                System.out.println("エラー内容   : " + errorMessage);
            }

            // 正常終了・異常終了のどちらでも、経過時間を秒単位の小数点以下2桁で表示します。
            double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf(Locale.ROOT, "処理時間     : %.2f秒%n", elapsedSeconds);
        }

        // 異常終了の場合は終了コード1を返し、正常終了の場合はそのまま終了します。
        if (exitCode != 0) {
            System.exit(1);
        }
    }

    /**
     * 抽出済みの試合の対戦チーム、日本時間の開始時刻、試合状態を表示します。
     *
     * @param games 日本時間の対象日に該当する試合一覧
     */
    private static void printGames(ArrayNode games) {
        // 試合一覧の見出しを、前後に空行を入れて表示します。
        System.out.println();
        System.out.println("--- 試合一覧 ---");
        System.out.println();

        // 試合が0件の場合は、対象日の試合がないことを表示します。
        if (games.isEmpty()) {
            System.out.println("対象日の試合はありません。");
            System.out.println();
            return;
        }

        // 日本時間の日時から時刻を整形し、抽出済みの順序で各試合を表示します。
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        int gameNumber = 1;
        for (JsonNode game : games) {
            String awayTeam = game.path("awayTeam").asText("-");
            String homeTeam = game.path("homeTeam").asText("-");
            String startTime = OffsetDateTime.parse(game.path("gameDateJst").asText())
                    .format(timeFormatter);
            String status = game.path("status").asText("-");
            System.out.println(gameNumber + ". " + awayTeam + " @ " + homeTeam);
            System.out.println("   開始時刻 : " + startTime + " JST");
            System.out.println("   状態     : " + status);
            System.out.println();
            gameNumber++;
        }
    }

    /**
     * APIレスポンスから日本時間の対象日に開始する試合を抽出し、重複を除いて追加します。
     *
     * @param json APIが返したJSON文字列
     * @param targetDate 日本時間の対象日
     * @param mapper JSONの解析と作成に使用するObjectMapper
     * @param games 抽出した試合の追加先
     * @param gamePks 採用済みの試合ID
     * @throws IOException JSONの解析に失敗した場合、または必須項目が不正な場合
     * @throws DateTimeException 試合開始日時の解析に失敗した場合
     */
    private static void collectGames(String json, LocalDate targetDate, ObjectMapper mapper,
            ArrayNode games, Set<Long> gamePks) throws IOException {
        // JSONをツリーとして解析し、試合日ごとの配列があることを確認します。
        JsonNode root = mapper.readTree(json);
        if (root == null || !root.isObject() || !root.path("dates").isArray()) {
            throw new IOException("レスポンスのdates配列が不正です。");
        }
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");

        // 各試合日のgames配列を順に読み取り、試合IDと開始日時を確認します。
        for (JsonNode day : root.path("dates")) {
            if (!day.path("games").isArray()) {
                throw new IOException("レスポンスのgames配列が不正です。");
            }
            for (JsonNode game : day.path("games")) {
                if (!game.path("gamePk").isIntegralNumber() || !game.path("gamePk").canConvertToLong()
                        || !game.path("gameDate").isTextual()) {
                    throw new IOException("試合のgamePkまたはgameDateが不正です。");
                }

                // UTCの開始日時を日本時間へ変換し、対象日以外の試合を除外します。
                String gameDateText = game.path("gameDate").asText();
                ZonedDateTime gameDateJst = Instant.parse(gameDateText).atZone(tokyoZone);
                if (!gameDateJst.toLocalDate().equals(targetDate)) {
                    continue;
                }

                // gamePkを集合へ追加し、すでに採用した試合は重複して追加しないようにします。
                long gamePk = game.path("gamePk").asLong();
                if (!gamePks.add(gamePk)) {
                    continue;
                }

                // 必要な項目だけを転記し、任意項目がない場合はJSONのnullとして保存します。
                ObjectNode selectedGame = mapper.createObjectNode();
                selectedGame.put("gamePk", gamePk);
                selectedGame.put("gameDate", gameDateText);
                selectedGame.put("gameDateJst", gameDateJst.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                selectedGame.put("awayTeam", game.path("teams").path("away").path("team").path("name").asText(null));
                selectedGame.put("homeTeam", game.path("teams").path("home").path("team").path("name").asText(null));
                selectedGame.put("status", game.path("status").path("detailedState").asText(null));
                selectedGame.put("venue", game.path("venue").path("name").asText(null));
                selectedGame.put("seriesDescription", game.path("seriesDescription").asText(null));
                selectedGame.set("seriesGameNumber", game.path("seriesGameNumber"));
                games.add(selectedGame);
            }
        }
    }

    /**
     * 指定した日付のMLBの試合一覧をHTTPレスポンスとして取得します。
     *
     * @param date APIで検索する試合日
     * @return HTTPステータスと未加工のJSON文字列を含むレスポンス
     * @throws IOException 通信に失敗した場合
     * @throws InterruptedException 通信中にスレッドが割り込まれた場合
     */
    private static HttpResponse<String> fetchGames(LocalDate date) throws IOException, InterruptedException {
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
        return client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * JSON文字列を指定したファイルにUTF-8で保存します。
     *
     * @param json 保存するJSON文字列
     * @param outputFile 日付を含む出力先の相対パス
     * @throws IOException フォルダの作成またはファイルへの書き込みに失敗した場合
     */
    private static void saveJson(String json, Path outputFile) throws IOException {
        // 出力フォルダが存在しない場合は作成します。
        Files.createDirectories(outputFile.getParent());

        // JSONをUTF-8で書き込み、既存ファイルがある場合は上書きします。
        Files.writeString(outputFile, json, StandardCharsets.UTF_8);
    }
}
