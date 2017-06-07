package models

/**
  * Created by geoffreywatson on 29/04/2017.
  */

case class ApplicationReview(loanApplication:LoanApplication,userCompany:UserCompany,company:Company,
                             companyAddress:CompanyAddress, coAddress:Address, user:User, userAddrss:UserAddress, userAdrress:Address)
