package de.timomeh.podcasts.models;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by Timo Maemecke (@timomeh) on 24/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
@RealmClass
public class Podcast extends RealmObject {

    @PrimaryKey
    private String uuid;

    private String feedurl;
    private String lastmodified;
    private String etag;
    private String title;
    private String author;
    private String imageurl;
    private String weburl;
    private String subtitle;
    private String summary;
    private boolean explicit;
    private boolean uptodate;

    // One-to-many Relationship (Podcast to Episodes)
    private RealmList<Episode> episodes;

    public Podcast() {
        this.feedurl = "";
        this.title = "";
        this.author = "";
        this.imageurl = "";
        this.weburl = "";
        this.subtitle = "";
        this.summary = "";
        this.explicit = false;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFeedurl() {
        return feedurl;
    }

    public void setFeedurl(String feedurl) {
        this.feedurl = feedurl;
    }

    public String getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(String lastmodified) {
        this.lastmodified = lastmodified;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImageurl() {
        return imageurl;
    }

    public void setImageurl(String imageurl) {
        this.imageurl = imageurl;
    }

    public String getWeburl() {
        return weburl;
    }

    public void setWeburl(String weburl) {
        this.weburl = weburl;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public RealmList<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(RealmList<Episode> episodes) {
        this.episodes = episodes;
    }

    public boolean isUptodate() {
        return uptodate;
    }

    public void setUptodate(boolean uptodate) {
        this.uptodate = uptodate;
    }
}
