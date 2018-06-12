package ml.melun.junhea.uboxdownloader;

import android.support.annotation.Nullable;

import org.json.JSONObject;

/*type
 * -1 = header
 * 0 = song search result
 * 1 = artist search result
 * 2 = artist from song
 * 3 = song from artist
 */
public class Item {
    private String name, thumb, key, artist;
    private int id, type;
    public Item(@Nullable int identification, String nameofitem, int typeofitem, @Nullable String thumbnaillink,
                @Nullable String streamkey, @Nullable String artistname){
        key = streamkey;
        id = identification;
        name = nameofitem;
        type = typeofitem;
        thumb = thumbnaillink;
        artist = artistname;
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

    public String getArtist(){
        return artist;
    }

    public String getJSON(){
        String data = "";
        try {
            data = new JSONObject()
                    .put("id", id)
                    .put("name", name)
                    .put("artist",artist)
                    .put("thumb",thumb)
                    .put("key",key).toString();
        }catch(Exception e){

        }
        return data;
    }
}
