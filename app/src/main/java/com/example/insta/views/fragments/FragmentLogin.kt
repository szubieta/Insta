package com.example.insta.views.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.insta.R
import com.example.insta.api.MiRetrofitBuilder
import com.example.insta.models.LoginUser
import com.example.insta.models.Token
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        userLogged = context?.getSharedPreferences("userLogged", Context.MODE_PRIVATE)

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

        if(registro) txtEstadoRegistroLogin.text = "Usuario registrado correctamente"

        btnLogin.setOnClickListener{
            if(txtUsername.text.isEmpty() || txtPassword.text.isEmpty()) {
                showToast(R.string.errorCamposRellenos)
            } else{
                Log.d("DEBUG", "${txtUsername.text}, ${txtPassword.text}")
                CoroutineScope(IO).launch{
                    getToken().enqueue(
                        object : Callback<Token> {
                            override fun onFailure(call: Call<Token>?, t: Throwable?) {
                                showToast(R.string.errorServerConnect)
                            }
                            override fun onResponse(call: Call<Token>?, response: Response<Token>?) {
                                if(response?.code()==200) {
                                    userLogged?.edit()?.putString("token", response.body()?.token)?.apply()
                                }
                                else {
                                    setTxtError("No se puede iniciar sesión con los datos proporcionados")
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

    private fun gotoRegisterFragment(){
        val bundle = bundleOf("hola" to "hola")
        navController.navigate(R.id.action_fragmentLogin_to_fragmentRegister2, bundle)
    }

    private fun getToken(): Call<Token>{
        return MiRetrofitBuilder.apiService.loginUser(LoginUser(txtUsername.text.toString(), txtPassword.text.toString()))
    }

    //metodo para comprobar el formato de email
    private fun CharSequence.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
    private fun CharSequence.isValid() = !isNullOrEmpty()

    private fun showToast(text: Int){
        CoroutineScope(Main).launch{
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
    private fun setTxtError(txt: String){
        CoroutineScope(Main).launch { txtEstadoRegistroLogin.text= txt ; txtEstadoRegistroLogin.setTextColor(resources.getColor(R.color.errorRed, context!!.theme))}
    }
}
