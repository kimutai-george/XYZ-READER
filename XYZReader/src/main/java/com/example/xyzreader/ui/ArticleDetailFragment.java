package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final int DETAIL_LOADER_ID = 0;
    private final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";

    private long mItemId;
    private Cursor mCursor;
    private View mRootView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private String mBodyText;
    private String mArticleTitle;
    private Spanned mAuthorName;
    private Toolbar mToolbar;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private ProgressBar mLoadingCircle;
    private WebView bodyView;
    private TextView titleView;
    private TextView bylineView;
    private ImageView mPhotoView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(generateSharedText(mArticleTitle, mAuthorName, mBodyText))
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        return mRootView;
    }


    /**
     * Generates a properly formatted string of text on the selected article for sharing
     * @param articleTitle The Title of the article
     * @param articleAuthor The author of the article
     * @param articleBody The content of the article
     * @return String The formatted shareable text
     */
    private String generateSharedText(String articleTitle, Spanned articleAuthor, String articleBody) {
        StringBuilder stringBuilder = new StringBuilder();
        if (articleAuthor != null) {
            stringBuilder.append(articleTitle);
        }
        if (articleAuthor != null) {
            stringBuilder.append("\n");
            stringBuilder.append(articleAuthor);
        }
        if (articleBody != null) {
            stringBuilder.append("\n\n");
            stringBuilder.append(articleBody);
        }

        return stringBuilder.toString();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindViews();

        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                int scrollPos = appBarLayout.getTotalScrollRange();
                if (verticalOffset + scrollPos == 0 && !getResources().getBoolean(R.bool.is_sw600)) {
                    mCollapsingToolbar.setTitle(mArticleTitle);
                } else {
                    mCollapsingToolbar.setTitle(" ");
                }
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mPhotoView = mRootView.findViewById(R.id.photo);
        mAppBar = mRootView.findViewById(R.id.app_bar);
        mCollapsingToolbar = mRootView.findViewById(R.id.collapsing_toolbar);
        mToolbar = mRootView.findViewById(R.id.toolbar_detail);
        mLoadingCircle = mRootView.findViewById(R.id.pb_detail);
        titleView = mRootView.findViewById(R.id.article_title);
        bylineView = mRootView.findViewById(R.id.article_byline);
        bodyView = mRootView.findViewById(R.id.article_body);

        mLoadingCircle.setVisibility(View.VISIBLE);
        bylineView.setMovementMethod(new LinkMovementMethod());

        if (getResources().getBoolean(R.bool.is_sw600)) {
            WebSettings webSettings = bodyView.getSettings();
            webSettings.setDefaultFontSize(22);
        }

        if (mCursor != null) {
            mRootView.setVisibility(View.VISIBLE);
            mArticleTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(mArticleTitle);
            Date publishedDate = parsePublishedDate();
            // If it's a tablet layout then change the author color to grey
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                if (getResources().getBoolean(R.bool.is_sw600)) {
                    mAuthorName = Html.fromHtml(
                            DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL).toString()
                                    + " by <font color='#\n" +
                                    "A6CACC'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>");
                } else {
                    mAuthorName = Html.fromHtml(
                            DateUtils.getRelativeTimeSpanString(
                                    publishedDate.getTime(),
                                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL).toString()
                                    + " by <font color='#ffffff'>"
                                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                    + "</font>");
                }
                bylineView.setText(mAuthorName);

            } else {
                // If date is before 1902, just show the string
                mAuthorName = Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");
                bylineView.setText(mAuthorName);

            }

            mBodyText = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString();
            bodyView.loadData(mBodyText, "text/html", "UTF-8");

            Picasso.with(getContext())
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .into(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
        }
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data == null || data.isClosed() || !data.moveToFirst()) return;

        if (mToolbar != null) {
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });
        }

        mCursor = data;

        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        mLoadingCircle.setVisibility(View.GONE);

        bindViews();
        rerunAnimations();
    }

    /**
     * Re-runs all the animations that you see when launching the ArticleDetailActivity (Image & Title Animations)
     */
    private void rerunAnimations() {
        Animation imageFadeAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.detail_img_fadein_anim);
        Animation byLineAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.detail_text_dropdown_anim);

        mPhotoView.setAnimation(imageFadeAnimation);
        bylineView.setAnimation(byLineAnimation);

        mPhotoView.animate();
        bylineView.animate();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mCursor = null;
    }
}
