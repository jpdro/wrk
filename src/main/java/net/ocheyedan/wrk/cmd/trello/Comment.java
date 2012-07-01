package net.ocheyedan.wrk.cmd.trello;

import net.ocheyedan.wrk.Config;
import net.ocheyedan.wrk.Output;
import net.ocheyedan.wrk.RestTemplate;
import net.ocheyedan.wrk.cmd.Args;
import net.ocheyedan.wrk.cmd.Usage;
import net.ocheyedan.wrk.trello.Action;
import net.ocheyedan.wrk.trello.TrelloUtil;
import org.codehaus.jackson.type.TypeReference;

import java.io.*;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * User: blangel
 * Date: 7/1/12
 * Time: 8:43 AM
 */
public final class Comment extends IdCommand {

    private final String url;

    private final String description;

    public Comment(Args args) {
        super(args);
        if ((args.args.size() == 3) && "on".equals(args.args.get(0))) {
            TrelloId cardId = parseWrkId(args.args.get(1), cardsPrefix);
            String comment = validate(encode(args.args.get(2)));
            url = TrelloUtil.url("https://trello.com/1/cards/%s/actions/comments?text=%s&key=%s&token=%s", cardId.id,
                                 comment, TrelloUtil.APP_DEV_KEY, TrelloUtil.USR_TOKEN);
            description = String.format("Commenting on card ^b^%s^r^:", cardId.id);
        } else if ((args.args.size() == 2) && "on".equals(args.args.get(0))) {
            TrelloId cardId = parseWrkId(args.args.get(1), cardsPrefix);
            String comment = validate(encode(getComment()));
            url = TrelloUtil.url("https://trello.com/1/cards/%s/actions/comments?text=%s&key=%s&token=%s", cardId.id,
                                 comment, TrelloUtil.APP_DEV_KEY, TrelloUtil.USR_TOKEN);
            description = String.format("Commenting on card ^b^%s^r^:", cardId.id);
        } else {
            url = description = null;
        }
    }

    private String validate(String comment) {
        if ((comment == null) || comment.isEmpty()) {
            Output.print("^red^Comment was empty, doing nothing.^r^");
            System.exit(0);
        }
        if (comment.length() > 16384) {
            Output.print("^red^Trello comments must be less than 16,384 characters, shortening.^r^");
            return comment.substring(0, 16384);
        }
        return comment;
    }

    private String encode(String comment) {
        try {
            return URLEncoder.encode(comment, "UTF-8");
        } catch (IOException ioe) {
            Output.print(ioe);
        }
        return comment;
    }

    private String getComment() {
        String editor = Config.getEditor();
        if ((editor == null) || editor.isEmpty()) {
            Output.print("^red^No editor defined within ~/.wrk/config, add an editor. For instance;^r^");
            Output.print("^b^{ \"color\": true, \"editor\": \"emacs\" }^r^");
            System.exit(1);
        }
        try {
            File temp = File.createTempFile("wrk", ".comment");
            String editorCommand = String.format("%s -nw -Q %s < /dev/tty", editor, temp.getPath());
            Process process = new ProcessBuilder("/bin/sh", "-c", editorCommand).redirectErrorStream(true).start();
            int result = process.waitFor();
            if (result != 0) {
                Output.print("^red^Editor exit code %d^r^", result);
                System.exit(result);
            }
            Scanner scanner = new Scanner(temp).useDelimiter("\\Z");
            if (!scanner.hasNext()) {
                return "";
            }
            return scanner.next();
        } catch (IOException ioe) {
            Output.print(ioe);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        System.exit(1); return null;
    }

    @Override protected Map<String, String> _run() {
        if (url == null) {
            new Usage(args).run();
            return Collections.emptyMap();
        }
        Output.print(description);
        Map<String, Object> result = RestTemplate.post(url, new TypeReference<Map<String, Object>>() {
        });
        if (result == null) {
            Output.print("  ^red^Invalid id or insufficient privileges.^r^");
        } else {
            Output.print("  ^b^Commented!^r^", result);
        }
        return Collections.emptyMap();
    }
}
