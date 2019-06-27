package un.eagle.elsa.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import un.eagle.elsa.*
import un.eagle.elsa.activities.FollowersActivity
import un.eagle.elsa.activities.FollowingActivity
import un.eagle.elsa.activities.SignInActivity
import un.eagle.elsa.graphql.Client

class ProfileFragment : Fragment() {

    companion object {
        const val TAG = "Eagle.ProfileFragment"
    }

    private fun goToFollowersActivity() {
        activity?.let {
            val intent = Intent (it, FollowersActivity::class.java)
            it.startActivity(intent)
        }
    }

    private fun goToFollowingActivity() {
        activity?.let {
            val intent = Intent (it, FollowingActivity::class.java)
            it.startActivity(intent)
        }
    }

    private fun goToUpdateDataActivity() {
        activity?.let {
            //TODO
        }
    }

    private fun logOut() {

        activity?.let {
            ElsaPreferences.deleteUserId(it)
            ElsaPreferences.deleteSessionJwt(it)
            val intent = Intent(it, SignInActivity::class.java)
            it.startActivity(intent)
        }
    }

    private fun loadFragment(fragment: Fragment) : Boolean {
        activity!!.supportFragmentManager.beginTransaction().replace(R.id.profileFragment_fragmentContainer,fragment).commit()
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.title = getString(R.string.title_profile)
        val v : View = inflater.inflate(R.layout.fragment_profile,null)

        val updateDataButton : Button   = v.findViewById(R.id.profile_updateData_button)
        val logOutButton     : Button   = v.findViewById(R.id.profile_logOut_button)
        val nameTV           : TextView = v.findViewById(R.id.profile_name_textView)
        val lastNameTV       : TextView = v.findViewById(R.id.profile_lastName_textView)
        val emailTV          : TextView = v.findViewById(R.id.profile_email_textView)
        val followersTV      : TextView = v.findViewById(R.id.profile_followers_textView)
        val followingTV      : TextView = v.findViewById(R.id.profile_following_textView)
        val musicList        : TextView = v.findViewById(R.id.profile_music_list_name)
        val musicListUrl     : TextView = v.findViewById(R.id.profile_music_list_url)

        followersTV.text = ""
        followingTV.text = ""
        nameTV.text = ""
        emailTV.text = ""
        lastNameTV.text = ""
        musicList.text = ""

        logOutButton.setOnClickListener { logOut() }
        updateDataButton.setOnClickListener { goToUpdateDataActivity() }
        followersTV.setOnClickListener { goToFollowersActivity() } //TODO
        followingTV.setOnClickListener { goToFollowingActivity() } //TODO


        val activity = activity!!
        val userId = ElsaPreferences.getUserId(activity)


        val callbackFollowers = object : ApolloCall.Callback<FollowersQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                Log.d(TAG, "Could not load following of $userId")
            }

            override fun onResponse(response: Response<FollowersQuery.Data>) {
                Log.d(TAG, "Successfully loaded following of $userId")
                val count = response.data()?.followers()?.count()!!
                activity.runOnUiThread{
                    followersTV.text = count.toString()
                }

            }
        }


        val callbackFollowing = object : ApolloCall.Callback<FollowingQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                Log.d(TAG, "Could not load followers of $userId")
            }

            override fun onResponse(response: Response<FollowingQuery.Data>) {
                Log.d(TAG, "Successfully loaded followers of $userId")
                val count = response.data()?.following()?.count()
                activity.runOnUiThread{
                    followingTV.text = count?.toString()
                }
            }
        }

        val callbackUser = object : ApolloCall.Callback<UserByIdQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                Log.d(TAG, "Failed to query user by id", e)
            }

            override fun onResponse(response: Response<UserByIdQuery.Data>) {
                val user = response.data()?.userById()!!
                activity.runOnUiThread{
                    emailTV.text = user.email()
                    nameTV.text = user.name()
                    lastNameTV.text = user.last_name()
                }

            }
        }

        val callbackFede = object : ApolloCall.Callback<GetMusicListQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                Log.d(TAG, "Failed Fede's query")
                musicListUrl.visibility = View.GONE
                musicList.text = getString(R.string.no_music_list)

            }

            override fun onResponse(response: Response<GetMusicListQuery.Data>) {
                val playlistName = response.data()?.musicList?.name()
                val playlistUrl = response.data()?.musicList?.url()
                if ( playlistName != null && playlistUrl != null ) {
                    musicListUrl.text = playlistUrl
                    musicList.text = getString(R.string.music_list, playlistName)
                    musicListUrl.visibility = View.VISIBLE
                }
                else {
                    musicListUrl.visibility = View.GONE
                    musicList.text = getString(R.string.no_music_list)
                }
            }
        }

        val profileFeed = PostsFragment()
        val args = Bundle()
        args.putInt(PostsFragment.TYPE, PostsFragment.PROFILE)
        profileFeed.arguments = args
        loadFragment(profileFeed)


        Client.queryFollowersFor(userId, callbackFollowers)
        Client.queryFollowingFor(userId, callbackFollowing)
        Client.queryUserById(userId, callbackUser)
        Client.getPlaylist(userId, callbackFede)

        return v
    }
}