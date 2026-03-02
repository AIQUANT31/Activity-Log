<<<<<<< HEAD
Frontend Run --> ng serve   
Backend Run  --> mvn spring-boot:run

=======




TO run the Frontend 
 Into the terminal write the command cd frontend 
                                      ng serve

  to run the backend
    wirte the command --- > cd backend 

                           mvn spring-boot:run


Api testing for the Tender 

  /api/tenders/all     Get
  /api/temder/5        Get
  /api/tender/create



  /api/tenders/create	POST		Tender created (ID: 6, budget: 75000)
/api/tenders/all	GET		Returns all 6 tenders
/api/tenders/5	GET		Returns single tender with budget
/api/tenders/user/1	GET		Returns user-specific tenders 



  

 For the OCR 

    Google Documnet Ai   
      Project id  = tender-487906   


      processsor id=582a9f90ef4a0f0a
>>>>>>> 4bb4a23 (update code with valiadtion phase)
POST /api/bids/upload-as-zip



if you share your project to the another there not apply jwt 

add the code into the backend server.address=0.0.0.0   
and in the frontend run   ng serve  --host 0.0.0.0