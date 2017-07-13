package tasks


import play.api.inject.{SimpleModule,_}

/**
  * Created by geoffreywatson on 10/07/2017.
  */



class TasksModule extends SimpleModule(bind[MyActorTask].toSelf.eagerly()) {

}
