package com.example.xyzreader.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.ui.DynamicHeightNetworkImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ArticleListHolder> {
    private Cursor cursor;
    private Activity activity;
    private int lastAnimPosition = -1;
    private boolean isSdkCompatible = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private final int DEFAULT_COLOR = 0xFF546E7A;

    public ArticleListAdapter(Activity a, Cursor c) {
        activity = a;
        cursor = c;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            isSdkCompatible = true;

        }
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ArticleListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
        final ArticleListHolder vh = new ArticleListHolder(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = vh.getAdapterPosition();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(activity, vh.thumbnailView, activity.getString(R.string.article_list_image_transition));
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(position))), transitionActivityOptions.toBundle());


                } else {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));


                }
            }
        });
        return vh;
    }

    private Date parsePublishedDate() {
        try {
            String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            return new Date();
        }
    }


    @Override
    public void onBindViewHolder(final ArticleListHolder holder, int position) {
        cursor.moveToPosition(position);
        Date publishedDate = parsePublishedDate();
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            holder.subtitleView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + "<br/>" + " by "
                            + cursor.getString(ArticleLoader.Query.AUTHOR)));
        } else {
            holder.subtitleView.setText(Html.fromHtml(
                    outputFormat.format(publishedDate)
                            + "<br/>" + " by "
                            + cursor.getString(ArticleLoader.Query.AUTHOR)));
        }
        holder.titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));
        final String imageUrl = cursor.getString(ArticleLoader.Query.THUMB_URL);
        holder.thumbnailView.setAspectRatio(cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        Glide.clear(holder.thumbnailView);
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.empty_detail)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        Bitmap bitmap = ((GlideBitmapDrawable) resource.getCurrent()).getBitmap();
                        Palette p = Palette.from(bitmap).generate();
                        int mMutedColor = p.getDarkVibrantColor(DEFAULT_COLOR);
                        holder.mutedColor = mMutedColor;
                        holder.itemView.setBackgroundColor(mMutedColor);
                        holder.thumbnailView.setBackgroundColor(mMutedColor);
                        return false;
                    }
                })
                .into(holder.thumbnailView);


//        if (isSdkCompatible) {
//            runAnimation(holder.itemView, position);
//        }


    }

    private void runAnimation(View viewToAnimate, int position) {
        if (position > lastAnimPosition) {
            Animation animation = AnimationUtils.loadAnimation(activity, android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastAnimPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(final ArticleListHolder holder) {
        if (isSdkCompatible)
            holder.itemView.clearAnimation();
    }


    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    class ArticleListHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle)
        TextView subtitleView;
        int mutedColor;


        ArticleListHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
            titleView.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "Rosario-Regular.ttf"));
            subtitleView.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "Rosario-Regular.ttf"));


        }

    }


}
