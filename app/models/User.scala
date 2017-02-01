package models

/**
  * Created by geoffreywatson on 01/02/2017.
  */
case class User(id:Long,email:String,password:String,var role:String="user")

case class UserFormData(email:String,password:String,confirmPswd:Option[String])




