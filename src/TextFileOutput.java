import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 【ソースファイル名】
 * TextFileOutput.java
 *
 * 【概要】
 * 実行時の作業フォルダ内のfileフォルダに、UTF-8のテキストファイルを出力します。
 * 同名ファイルがある場合は上書きし、try-with-resourcesでファイルを自動的に閉じます。
 *
 * 【作成日】2026-09-22
 * 【最終更新日】2026-09-22
 *
 * @author masa
 * @version 1.0
 */
public class TextFileOutput {

    /**
     * テキストファイルを出力し、結果をコンソールに表示します。
     * 出力に失敗した場合は、エラー内容を標準エラー出力に表示して終了コード1で終了します。
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        // 作業フォルダを基準に、出力先を絶対パスで取得します。
        Path outputPath = Path.of("file", "output.txt").toAbsolutePath().normalize();

        try {
            // 出力先のフォルダが存在しない場合は作成します。
            Files.createDirectories(outputPath.getParent());

            // UTF-8でファイルを開き、新規作成または上書きします。
            // try-with-resourcesにより、例外が発生した場合もライターを自動的に閉じます。
            try (BufferedWriter writer = Files.newBufferedWriter(
                    outputPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {

                // 文字列を書き込み、実行環境に応じた改行を出力します。
                writer.write("Hello, World!");
                writer.newLine();
                writer.write("こんにちは、Java！");
                writer.newLine();
                writer.write("UTF-8でテキストファイルを出力しました。");
                writer.newLine();
            }

            // 書き込みとクローズが正常に完了した後に、保存先を表示します。
            System.out.println("ファイルを出力しました: " + outputPath);
        } catch (IOException e) {
            // 作成・書き込み・クローズに失敗した場合は、保存先と原因を表示します。
            System.err.println("ファイルの出力に失敗しました: " + outputPath);
            e.printStackTrace(System.err);

            // 呼び出し元が失敗を判定できるように、終了コード1で終了します。
            System.exit(1);
        }
    }
}
