import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 【ソースファイル名】
 * TextFileInput.java
 *
 * 【概要】
 * 実行時の作業フォルダ内のfileフォルダにあるUTF-8のテキストファイルを読み込みます。
 * 読み込んだ内容は、ファイル名と文字コードを表示した後に標準出力へ出力します。
 *
 * 【作成日】2026-09-22
 * 【最終更新日】2026-09-22
 *
 * @author masa
 * @version 1.0
 */
public class TextFileInput {

    /**
     * テキストファイルを読み込み、ファイル情報と内容をコンソールに表示します。
     * 読み込みに失敗した場合は、エラー内容を標準エラー出力に表示して終了コード1で終了します。
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        // 作業フォルダを基準に、読み込み対象のファイルを絶対パスで取得します。
        Path inputPath = Path.of("file", "output.txt").toAbsolutePath().normalize();

        try {
            // 読み込み対象のファイル名を標準出力に表示します。
            System.out.println("ファイル名: " + inputPath.getFileName());

            // 読み込みに使用する文字コードを標準出力に表示します。
            System.out.println("文字コード: " + StandardCharsets.UTF_8);

            // ファイル内容の表示を分かりやすくするため、見出しを標準出力に表示します。
            System.out.println("ファイル内容:");

            // UTF-8でファイルを開き、1行ずつ読み込みます。
            // try-with-resourcesにより、例外が発生した場合もリーダーを自動的に閉じます。
            try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
                String line;

                // 読み込む行がなくなるまで、1行ずつ標準出力に表示します。
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            // ファイルのオープン・読み込み・クローズに失敗した場合は、対象ファイルと原因を表示します。
            System.err.println("ファイルの読み込みに失敗しました: " + inputPath);
            e.printStackTrace(System.err);

            // 呼び出し元が失敗を判定できるように、終了コード1で終了します。
            System.exit(1);
        }
    }
}
