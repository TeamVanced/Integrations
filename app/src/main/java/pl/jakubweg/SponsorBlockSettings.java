package pl.jakubweg;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static pl.jakubweg.StringRef.sf;

public class SponsorBlockSettings {

    public static final String PREFERENCES_NAME = "sponsor-block";
    public static final String PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP = "show-toast";
    public static final String PREFERENCES_KEY_COUNT_SKIPS = "count-skips";
    public static final String PREFERENCES_KEY_SKIP_ON_CLICK = "skip-on-click";
    public static final String PREFERENCES_KEY_UUID = "uuid";
    public static final String PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP = "new-segment-step-accuracy";
    public static final String PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED = "sb-enabled";
    public static final String PREFERENCES_KEY_SEEN_GUIDELINES = "sb-seen-gl";
    public static final String PREFERENCES_KEY_NEW_SEGMENT_ENABLED = "sb-new-segment-enabled";
    public static final String sponsorBlockSkipSegmentsUrl = "https://sponsor.ajay.app/api/skipSegments";
    public static final String sponsorBlockViewedUrl = "https://sponsor.ajay.app/api/viewedVideoSponsorTime";


    public static final SegmentBehaviour DefaultBehaviour = SegmentBehaviour.SkipAutomatically;

    public static boolean isSponsorBlockEnabled = false;
    public static boolean seenGuidelinesPopup = false;
    public static boolean isAddNewSegmentEnabled = false;
    public static boolean showToastWhenSkippedAutomatically = true;
    public static boolean countSkips = true;
    public static boolean skipOnClick = true;
    public static int adjustNewSegmentMillis = 150;
    public static String uuid = "<invalid>";
    private static String sponsorBlockUrlCategories = "[]";

    @SuppressWarnings("unused")
    @Deprecated
    public SponsorBlockSettings(Context ignored) {
        Log.e("jakubweg.Settings", "Do not call SponsorBlockSettings constructor!");
    }

    public static String getSponsorBlockUrlWithCategories(String videoId) {
        return sponsorBlockSkipSegmentsUrl + "?videoID=" + videoId + "&categories=" + sponsorBlockUrlCategories;
    }

    public static String getSponsorBlockViewedUrl(String UUID) {
        return sponsorBlockViewedUrl + "?UUID=" + UUID;
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static void setSeenGuidelines(Context context) {
        SponsorBlockSettings.seenGuidelinesPopup = true;
        getPreferences(context).edit().putBoolean(PREFERENCES_KEY_SEEN_GUIDELINES, true).apply();
    }

    public static void update(Context context) {
        if (context == null) return;

        SharedPreferences preferences = getPreferences(context);
        isSponsorBlockEnabled = preferences.getBoolean(PREFERENCES_KEY_SPONSOR_BLOCK_ENABLED, isSponsorBlockEnabled);
        seenGuidelinesPopup = preferences.getBoolean(PREFERENCES_KEY_SEEN_GUIDELINES, seenGuidelinesPopup);

        if (!isSponsorBlockEnabled) {
            SkipSegmentView.hide();
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideButton();
            PlayerController.sponsorSegmentsOfCurrentVideo = null;
        } else if (/*isAddNewSegmentEnabled*/false) {
            SponsorBlockUtils.showButton();
        }

        isAddNewSegmentEnabled = preferences.getBoolean(PREFERENCES_KEY_NEW_SEGMENT_ENABLED, isAddNewSegmentEnabled);
        if (!/*isAddNewSegmentEnabled*/false) {
            NewSegmentHelperLayout.hide();
            SponsorBlockUtils.hideButton();
        } else {
            SponsorBlockUtils.showButton();
        }

        SegmentBehaviour[] possibleBehaviours = SegmentBehaviour.values();
        final ArrayList<String> enabledCategories = new ArrayList<>(possibleBehaviours.length);
        for (SegmentInfo segment : SegmentInfo.valuesWithoutPreview()) {
            SegmentBehaviour behaviour = null;
            String value = preferences.getString(segment.key, null);
            if (value == null)
                behaviour = DefaultBehaviour;
            else {
                for (SegmentBehaviour possibleBehaviour : possibleBehaviours) {
                    if (possibleBehaviour.key.equals(value)) {
                        behaviour = possibleBehaviour;
                        break;
                    }
                }
            }
            if (behaviour == null)
                behaviour = DefaultBehaviour;

            segment.behaviour = behaviour;
            if (behaviour.showOnTimeBar)
                enabledCategories.add(segment.key);
        }

        //"[%22sponsor%22,%22outro%22,%22music_offtopic%22,%22intro%22,%22selfpromo%22,%22interaction%22]";
        if (enabledCategories.size() == 0)
            sponsorBlockUrlCategories = "[]";
        else
            sponsorBlockUrlCategories = "[%22" + TextUtils.join("%22,%22", enabledCategories) + "%22]";


        showToastWhenSkippedAutomatically = preferences.getBoolean(PREFERENCES_KEY_SHOW_TOAST_WHEN_SKIP, showToastWhenSkippedAutomatically);
        String tmp1 = preferences.getString(PREFERENCES_KEY_ADJUST_NEW_SEGMENT_STEP, null);
        if (tmp1 != null)
            adjustNewSegmentMillis = Integer.parseInt(tmp1);

        countSkips = preferences.getBoolean(PREFERENCES_KEY_COUNT_SKIPS, countSkips);
        skipOnClick = preferences.getBoolean(PREFERENCES_KEY_SKIP_ON_CLICK, skipOnClick);

        uuid = preferences.getString(PREFERENCES_KEY_UUID, null);
        if (uuid == null) {
            uuid = (UUID.randomUUID().toString() +
                    UUID.randomUUID().toString() +
                    UUID.randomUUID().toString())
                    .replace("-", "");
            preferences.edit().putString(PREFERENCES_KEY_UUID, uuid).apply();
        }
    }

    public enum SegmentBehaviour {
        SkipAutomatically("skip", sf("skip_automatically"), true, true),
        ManualSkip("manual-skip", sf("skip_showbutton"), false, true),
        Ignore("ignore", sf("skip_ignore"), false, false);

        public final String key;
        public final StringRef name;
        public final boolean skip;
        public final boolean showOnTimeBar;

        SegmentBehaviour(String key,
                         StringRef name,
                         boolean skip,
                         boolean showOnTimeBar) {
            this.key = key;
            this.name = name;
            this.skip = skip;
            this.showOnTimeBar = showOnTimeBar;
        }
    }

    public enum SegmentInfo {
        Sponsor("sponsor", sf("segments_sponsor"), sf("skipped_sponsor"), sf("segments_sponsor_sum"), null, 0xFF00d400),
        Intro("intro", sf("segments_intermission"), sf("skipped_intermission"), sf("segments_intermission_sum"), null, 0xFF00ffff),
        Outro("outro", sf("segments_endcards"), sf("skipped_endcard"), sf("segments_endcards_sum"), null, 0xFF0202ed),
        Interaction("interaction", sf("segments_subscribe"), sf("skipped_subscribe"), sf("segments_subscribe_sum"), null, 0xFFcc00ff),
        SelfPromo("selfpromo", sf("segments_selfpromo"), sf("skipped_selfpromo"), sf("segments_selfpromo_sum"), null, 0xFFffff00),
        MusicOfftopic("music_offtopic", sf("segments_nomusic"), sf("skipped_nomusic"), sf("segments_nomusic_sum"), null, 0xFFff9900),
        Preview("preview", StringRef.empty, sf("skipped_preview"), StringRef.empty, SegmentBehaviour.SkipAutomatically, 0xFF000000),
        ;

        private static SegmentInfo[] mValuesWithoutPreview = new SegmentInfo[]{
                Sponsor,
                Intro,
                Outro,
                Interaction,
                SelfPromo,
                MusicOfftopic
        };
        private static Map<String, SegmentInfo> mValuesMap = new HashMap<>(7);

        static {
            for (SegmentInfo value : valuesWithoutPreview())
                mValuesMap.put(value.key, value);
        }

        public final String key;
        public final StringRef title;
        public final StringRef skipMessage;
        public final StringRef description;
        public final int color;
        public final Paint paint;
        public SegmentBehaviour behaviour;
        private CharSequence lazyTitleWithDot;

        SegmentInfo(String key,
                    StringRef title,
                    StringRef skipMessage,
                    StringRef description,
                    SegmentBehaviour behaviour,
                    int color) {

            this.key = key;
            this.title = title;
            this.skipMessage = skipMessage;
            this.description = description;
            this.behaviour = behaviour;
            this.color = color & 0xFFFFFF;
            paint = new Paint();
            paint.setColor(color);
        }

        public static SegmentInfo[] valuesWithoutPreview() {
            return mValuesWithoutPreview;
        }

        public static SegmentInfo byCategoryKey(String key) {
            return mValuesMap.get(key);
        }

        public CharSequence getTitleWithDot() {
            return (lazyTitleWithDot == null) ?
                    lazyTitleWithDot = Html.fromHtml(String.format("<font color=\"#%06X\">⬤</font> %s", color, title))
                    : lazyTitleWithDot;
        }
    }
}
