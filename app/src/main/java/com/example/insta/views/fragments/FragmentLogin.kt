package com.example.insta.views.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.insta.R
import com.example.insta.api.MiRetrofitBuilder
import com.example.insta.models.LoginUser
import com.example.insta.models.Token
import com.example.insta.models.User
import com.example.insta.models.UserFollow
import com.example.insta.utils.MiSharedPreferences
import com.example.insta.utils.MiViewUtils
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentLogin : Fragment() {

    //atributo lateinit(inicializacion tardia) que nos permite navegar entre los fragmentos
    private lateinit var navController: NavController
    private var userLogged: SharedPreferences? = null
    //variable estatica publica para mostrar menesajes al usuario
    companion object{
        var registro = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO comprobar si existe token guardado para ir al siguiente fragmento
        //autenticar al usuario antes de crear la actividad
        /*if(true){
            val intent = Intent(this, Activity2::class.java)
            startActivity(intent)
            finish()
        }*/
        //
        Log.d("retrofit", MiSharedPreferences.getPreferencesString("email").toString())
        Log.d("retrofit", MiSharedPreferences.getPreferencesString("username").toString())
        Log.d("retrofit", MiSharedPreferences.getPreferencesString("nombre").toString())
        Log.d("retrofit", MiSharedPreferences.getPreferencesString("apellido1").toString())
        Log.d("retrofit", MiSharedPreferences.getPreferencesString("seguidos").toString())

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        if(registro) setTxtError(R.string.txtEstadoSuccess)

        btnLogin.setOnClickListener{
            if(!txtUsername.text.isValidEmail() || !txtPassword.text.isValid()) {
                showToast(R.string.errorCamposRellenos)
            } else{
                Log.d("DEBUG", "${txtUsername.text}, ${txtPassword.text}")
                CoroutineScope(IO).launch{
                    showLoadingDialog()
                    MiViewUtils.hideView(txtEstadoRegistroLogin)
                    disableRegisterButton()
                    getToken().enqueue(
                        object : Callback<Token> {
                            override fun onFailure(call: Call<Token>?, t: Throwable?) {

                                showToast(R.string.errorServerConnect)
                            }
                            override fun onResponse(call: Call<Token>?, response: Response<Token>?) {

                                if(response?.code()==200) {
                                    response.body()?.token?.let {token->
                                        Log.d("retrofit", token)
                                        MiSharedPreferences.editPreferencesString("token", token)
                                        getUserLogged(token).enqueue(
                                            object : Callback<User>{
                                                override fun onFailure(call: Call<User>, t: Throwable) {
                                                    showToast(R.string.errorServerConnect)
                                                    Log.d("retrofit", t.message.toString())
                                                }
                                                override fun onResponse(call: Call<User>, response: Response<User>) {
                                                    when(response.code()){
                                                        200->{
                                                            val user  = response.body()
                                                            MiSharedPreferences.editPreferencesString("email", user?.email.toString())
                                                            MiSharedPreferences.editPreferencesString("username", user?.username.toString())
                                                            MiSharedPreferences.editPreferencesString("nombre", user?.nombre.toString())
                                                            MiSharedPreferences.editPreferencesString("apellido1", user?.apellido1.toString())
                                                            MiSharedPreferences.editPreferencesString("apellido2", user?.apellido2.toString())
                                                            MiSharedPreferences.editPreferencesString("telefono", user?.telefono.toString())
                                                            MiSharedPreferences.editPreferencesString("imagen", user?.imagen.toString())
                                                            MiSharedPreferences.editPreferencesString("genero", user?.genero.toString())
                                                            MiSharedPreferences.editPreferencesString("n_seguidores", user?.n_seguidores.toString())
                                                            MiSharedPreferences.editPreferencesString("n_seguidos", user?.n_seguidos.toString())
                                                            MiSharedPreferences.editPreferencesString("descripcion", user?.descripcion.toString())
                                                            val gson = Gson()
                                                            val listUsuariosSeguidos = HashSet<UserFollow>()
                                                            user?.seguidos?.let { it ->
                                                                val jsonSeguidos = gson.toJson(it)
                                                                MiSharedPreferences.editPreferencesString("seguidos", jsonSeguidos)
                                                            }
                                                            val listUsuariosSeguidores = HashSet<UserFollow>()
                                                            user?.seguidores?.let { it ->
                                                                val jsonSeguidores = gson.toJson(it)
                                                                MiSharedPreferences.editPreferencesString("seguidores", jsonSeguidores)
                                                            }
                                                            hideLoadingDialog()
                                                        }
                                                        401->{
                                                            Log.d("retrofit", response.errorBody()?.string().toString())
                                                            hideLoadingDialog()
                                                            enableRegisterButton()
                                                            setTxtError(R.string.errorLoginCredentials)
                                                            showView(txtEstadoRegistroLogin)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                else {
                                    setTxtError(R.string.errorLoginCredentials)
                                    hideLoadingDialog()
                                    enableRegisterButton()
                                    showView(txtEstadoRegistroLogin)
                                }
                            }
                        })
                }
            }
        }

        btnRegistro.setOnClickListener{
            CoroutineScope(Main).launch { gotoRegisterFragment() }
        }
    }

    private fun showLoadingDialog(){
        CoroutineScope(Main).launch { progressBarLogin.visibility = View.VISIBLE }
    }
    private fun hideLoadingDialog(){
        CoroutineScope(Main).launch { progressBarLogin.visibility = View.INVISIBLE }
    }
    private fun disableRegisterButton(){
        CoroutineScope(Main).launch { btnLogin.isEnabled = false }
    }
    private fun enableRegisterButton(){
        CoroutineScope(Main).launch { btnLogin.isEnabled = true }
    }
    private fun hideView(view: View){
        CoroutineScope(Main).launch {view.visibility = View.INVISIBLE}
    }
    private fun showView(view: View){
        CoroutineScope(Main).launch {view.visibility = View.VISIBLE}
    }


    private fun gotoRegisterFragment(){
        val bundle = bundleOf("hola" to "hola")
        navController.navigate(R.id.action_fragmentLogin_to_fragmentRegister2, bundle)
    }

    private fun getToken(): Call<Token>{
        return MiRetrofitBuilder.apiService.loginUser(LoginUser(txtUsername.text.toString(), txtPassword.text.toString()))
    }

    private fun getUserLogged(token: String): Call<User>{
        return MiRetrofitBuilder.apiService.getUserLogged("Token $token")
    }

    //metodo para comprobar el formato de email
    private fun CharSequence.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
    private fun CharSequence.isValid() = !isNullOrEmpty()

    private fun showToast(text: Int){
        CoroutineScope(Main).launch{
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    private fun setTxtError(txt: Int){
        CoroutineScope(Main).launch { txtEstadoRegistroLogin.text= resources.getString(txt) ; txtEstadoRegistroLogin.setTextColor(resources.getColor(R.color.errorRed, requireContext().theme))}
    }
}
