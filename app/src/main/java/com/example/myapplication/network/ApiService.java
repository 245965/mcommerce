package com.example.myapplication.network;

import com.example.myapplication.data.ClientSecretResponseDto;
import com.example.myapplication.data.EventParticipantDto;
import com.example.myapplication.data.ExpenseDto;
import com.example.myapplication.data.TaskDto;
import com.example.myapplication.data.ReminderRequest;
import com.example.myapplication.data.UserDto;
import com.example.myapplication.model.Event;
import com.example.myapplication.model.LoginResponse;
import com.example.myapplication.model.PaginatedResponse;
import com.example.myapplication.model.PollOption;
import com.example.myapplication.model.RegisterResponse;


import com.example.myapplication.data.CreateMessageDto;
import com.example.myapplication.data.MessageDto;
import com.example.myapplication.data.CreateExpenseDto;
import com.example.myapplication.model.Task;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface ApiService {

    @FormUrlEncoded
    @POST("/auth/register")
    Call<RegisterResponse> registerUser(
            @Field("email") String email,
            @Field("password") String password,
            @Field("firstName") String firstName,
            @Field("lastName") String lastName
    );

    @FormUrlEncoded
    @POST("/auth/login")
    Call<LoginResponse> loginUser(
            @Field("email") String email,
            @Field("password") String password
    );

    @GET("api/events")
    Call<PaginatedResponse<Event>> getEvents(@Query("page") int page, @Query("size") int size);

    @POST("api/events")
    Call<Event> createEvent(@Body Event event);

    @GET("/api/events/{id}")
    Call<Event> getEvent(@Path("id") Long eventId);

    @Multipart
    @POST("/api/events/with-image")
    Call<Event> createEventWithImage(
            @Part MultipartBody.Part image,
            @Part("event") RequestBody eventJson
    );

    @Multipart
    @POST("/api/users/avatar")
    Call<Map<String,String>> uploadAvatar(@Part MultipartBody.Part avatar);

    @FormUrlEncoded
    @PUT("/api/users")
    Call<Map<String,String>> updateProfile(
            @Field("firstName") String firstName,
            @Field("lastName")  String lastName
    );


    @GET("/api/users")
    Call<Map<String,String>> me();

    @GET("api/events/{eventId}/datePollOptions")
    Call<List<PollOption>> getDatePollOptions(@Path("eventId") Long eventId);

    @GET("api/events/{eventId}/locationPollOptions")
    Call<List<PollOption>> getLocationPollOptions(@Path("eventId") Long eventId);

    @POST("api/polls/{pollId}/options/{optionId}/vote")
    Call<Void> vote(@Path("pollId") Long pollId, @Path("optionId") Long optionId);


    @GET("/api/messages")
    Call<List<MessageDto>> getMessages(@Query("eventId") long eventId);

    @GET("/api/messages/latest")
    Call<List<MessageDto>> getLatest(@Query("eventId") long eventId,
                                     @Query("after") String afterIso);

    @POST("/api/messages")
    Call<MessageDto> sendMessage(@Body CreateMessageDto body);

    @GET("/api/expenses/event/{eventId}")
    Call<List<ExpenseDto>> getExpensesForEvent(@Path("eventId") Long eventId);

    @GET("/api/expenses/{eventId}/balances")
    Call<Map<String, Double>> getSettlement(@Path("eventId") Long eventId);

    @POST("/api/expenses")
    Call<ExpenseDto> addExpense(@Body CreateExpenseDto dto);


    @GET("/api/events/{eventId}/photos")
    Call<List<String>> getEventPhotos(@Path("eventId") long eventId);

    @Multipart
    @POST("/api/events/{eventId}/photos")
    Call<Void> uploadEventPhoto(@Path("eventId") long eventId,
                                @Part MultipartBody.Part file);


    @POST("/api/reminders")
    Call<Void> registerReminder(@Body ReminderRequest request);

    @POST("/api/notifications/token")
    Call<Void> sendFcmToken(@Query("token") String token);

    @GET("/api/users/me")
    Call<UserDto> getCurrentUser();

    @GET("/api/tasks")
    Call<PaginatedResponse<TaskDto>> getTasks(@Query("page") int page, @Query("size") int size);

    @GET("/api/tasks/{taskId}")
    Call<TaskDto> getTask(@Path("taskId") Long taskId);

    @GET("/api/tasks")
    Call<PaginatedResponse<TaskDto>> getEventsTasks(@Query("page") int page, @Query("size") int size, @Query("eventId") Long eventId);

    @POST("/api/tasks")
    Call<TaskDto> addTask(@Query("eventId") Long eventId, @Body Task task);

    @POST("/api/tasks")
    Call<TaskDto> addTask(@Query("eventId") Long eventId, @Body Task task, @Query("assignedUserId") Long assignedUserId);

    @PUT("/api/tasks/{taskId}")
    Call<TaskDto> updateTask(@Path("taskId") Long taskId, @Body Task task);

    @PUT("/api/tasks/assign/{taskId}")
    Call<TaskDto> assignTask(@Path("taskId") Long taskId, @Query("assignedUserId") Long assignedUserId);

    @DELETE("/api/tasks/{taskId}")
    Call<Void> deleteTask(@Path("taskId") Long taskId);

    @GET("/api/events/{eventId}/participants")
    Call<List<String>> getEventParticipants(@Path("eventId") long eventId);

    @PUT("/api/events/{eventId}/participants/{userId}")
    Call<EventParticipantDto> joinEvent(@Path("eventId") Long eventId,
                                        @Path("userId") Long userId);

    @GET("/api/events/{id}/budget-deadline")
    Call<String> getBudgetDeadline(@Path("id") long eventId);

    @GET("/api/events/{eventId}/participants")
    Call<PaginatedResponse<EventParticipantDto>> getEventParticipants(@Path("eventId") Long eventId, @Query("page") int page, @Query("size") int size, @Query("eventRole") String eventRole);

    @PUT("/api/tasks/{taskId}/changeStatus")
    Call<TaskDto> taskChangeStatus(@Path("taskId") Long taskId, @Query("done") boolean done);

    @POST("/api/events/{eventId}/stripe-payment")
    Call<ClientSecretResponseDto> createStripePayment(@Path("eventId") Long eventId, @Query("amount") BigDecimal amount);
}
