package request;

import java.util.List;
import java.util.Set;

public class DingDingMsgRequest {
    public String msgtype;

    public At at;

    public Text text;

    @Override
    public String toString() {
        return "DingDingMsgRequest{" +
                "msgtype='" + msgtype + '\'' +
                ", at=" + at +
                ", text=" + text +
                '}';
    }

    public static final class At {
        public Boolean isAtAll;

        public Set<String> atMobiles;

        public At(Boolean isAtAll, Set<String> atMobiles) {
            this.isAtAll = isAtAll;
            this.atMobiles = atMobiles;
        }

        @Override
        public String toString() {
            return "At{" +
                    "isAtAll=" + isAtAll +
                    ", atMobiles=" + atMobiles +
                    '}';
        }
    }

    public static final class Text {
        public String content;

        public Text(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "Text{" +
                    "content='" + content + '\'' +
                    '}';
        }
    }
}
