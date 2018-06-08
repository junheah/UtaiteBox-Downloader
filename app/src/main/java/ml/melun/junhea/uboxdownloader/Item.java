package ml.melun.junhea.uboxdownloader;

import android.support.annotation.Nullable;
/*type
 * -1 = header
 * 0 = song search result
 * 1 = artist search result
 * 2 = artist from song
 * 3 = song from artist
 */
public class Item {
    private String name, thumb, key;
    private int id, type;
    public Item(@Nullable int identification, String nameofitem, int typeofitem, @Nullable String thumbnaillink,
                @Nullable String streamkey){
        key = streamkey;
        id = identification;
        name = nameofitem;
        type = typeofitem;
        thumb = thumbnaillink;
    }

    public int getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getThumb() {
        return thumb;
    }

    public String getKey() {
        return key;
    }
}