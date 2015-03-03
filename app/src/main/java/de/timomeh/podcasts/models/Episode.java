package de.timomeh.podcasts.models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

/**
 * Created by Timo Maemecke (@timomeh) on 24/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
@RealmClass
public class Episode extends RealmObject {

    @PrimaryKey
    private String uuid;

    private String guid;
    private String title;
    private String weburl;
    private String imageurl;
    private Date pubdate;
    private long stoppedAt;
    private long duration;
    private String summary;
    private boolean explicit;
    private boolean seen;

    // File properties
    private String fileurl;
    private String fileuri;
    private long filesize;
    private String filetype;

    // One-to-one Relationship (Episode to Podcast)
    private Podcast podcast;

    // This is totally useless to be stored in the database but as long as
    // Realm does not support non-saving fields, I need to save this. Duh.
    private boolean updated;

    public Episode() {
        this.guid = "";
        this.title = "";
        this.weburl = "";
        this.imageurl = "";
        this.pubdate = new Date();
        this.stoppedAt = 0;
        this.duration = 0;
        this.summary = "";
        this.explicit = false;
        this.seen = false;
        this.fileurl = "";
        this.fileuri = "";
        this.filesize = 0;
        this.filetype = "";
        this.updated = false;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWeburl() {
        return weburl;
    }

    public void setWeburl(String weburl) {
        this.weburl = weburl;
    }

    public String getImageurl() {
        return imageurl;
    }

    public void setImageurl(String imageurl) {
        this.imageurl = imageurl;
    }

    public Date getPubdate() {
        return pubdate;
    }

    public void setPubdate(Date pubdate) {
        this.pubdate = pubdate;
    }

    public long getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(long stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
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

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getFileurl() {
        return fileurl;
    }

    public void setFileurl(String fileurl) {
        this.fileurl = fileurl;
    }

    public String getFileuri() {
        return fileuri;
    }

    public void setFileuri(String fileuri) {
        this.fileuri = fileuri;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
