package com.mentos_koder.remote_lg_tv.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.mentos_koder.remote_lg_tv.R
import com.mentos_koder.remote_lg_tv.model.AudioFile
import com.mentos_koder.remote_lg_tv.util.Singleton
import com.mentos_koder.remote_lg_tv.view.fragment.DeviceFragment
import com.mentos_koder.remote_lg_tv.viewmodel.AudioViewModel
import com.mentos_koder.remote_lg_tv.view.fragment.PlayAudioFragment
import java.io.File

class AudioAdapter(
    private val audioList: List<AudioFile>,
    private val viewModel: AudioViewModel,
    private val context: Context
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audioList[position]
        holder.audioName.text = audio.name
        holder.audioArtists.text = audio.artist
        holder.audioDuration.text = formatDuration(audio.duration)
        if (audio.albumArt != null) {
            Glide.with(holder.itemView.context)
                .load(File(audio.albumArt))
                .apply(RequestOptions().placeholder(R.drawable.ic_app).error(R.drawable.ic_setting))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.audioAlbumArt)
        } else {
            holder.audioAlbumArt.setImageResource(R.drawable.ic_audio1)
        }

        holder.itemView.setOnClickListener {
            if (Singleton.getInstance().isConnected()) {
                viewModel.setSelectedAudio(audio)
                val newFragment = PlayAudioFragment()
                if (context is FragmentActivity) {
                    val transaction = context.supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container, newFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
            } else {
                val deviceFrag = DeviceFragment()
                if (context is FragmentActivity) {
                    val transaction = context.supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.slide_out_left
                        )
                    transaction.replace(R.id.fragment_container, deviceFrag, "findThisFragment")
                    transaction.addToBackStack("findThisFragment")
                    transaction.commit()
                }
            }
        }
    }

    override fun getItemCount() = audioList.size

    private fun formatDuration(duration: Long): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val audioName: TextView = view.findViewById(R.id.tv_audio_name)
        val audioDuration: TextView = view.findViewById(R.id.tv_audio_duration)
        val audioArtists: TextView = view.findViewById(R.id.tv_artist)
        val audioAlbumArt: ImageView = view.findViewById(R.id.audio_album_art)
    }
}