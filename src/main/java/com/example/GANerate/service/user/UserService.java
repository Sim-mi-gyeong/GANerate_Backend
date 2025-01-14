package com.example.GANerate.service.user;

import com.example.GANerate.config.jwt.TokenProvider;
import com.example.GANerate.config.redis.RedisUtil;
import com.example.GANerate.domain.*;
import com.example.GANerate.enumuration.Result;
import com.example.GANerate.exception.CustomException;
import com.example.GANerate.repository.HeartRepository;
import com.example.GANerate.repository.UserRepository;
import com.example.GANerate.request.user.UserRequest;
import com.example.GANerate.response.dateProduct.DataProductResponse;
import com.example.GANerate.response.user.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.example.GANerate.enumuration.Result.USERID_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisUtil redisUtil;

    //회원가입
    @Transactional
    public UserResponse.signup signup(UserRequest.signup request){
        //아이디 중복 검사
        validateDuplicatedUserEmail(request.getEmail());

        // 이메일 인증 검사
        if(request.isEmailAuth()!=true){
            throw new CustomException(Result.UN_AUTHENTICATION_EMAIL);
        }

        //저장
        User user = userRepository.save(createEntityUserFromDto(request));

        return UserResponse.signup.response(user);
    }

    //로그인
    @Transactional(readOnly = true)
    public UserResponse.signin signin(UserRequest.signin request){

        // 일치하는 userId 없음
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(USERID_NOT_FOUND));

        // password 틀림
        if(!passwordEncoder.matches(request.getUserPw(), user.getUserPw())){
            throw new CustomException(Result.INVALID_PASSWORD);
        }

        //앞에서 exception 안나면 access token 발행
        String accessToken = tokenProvider.createToken(user.getId(), getAuthentication(request.getEmail(), request.getUserPw()));
        String refreshToken = tokenProvider.createRefreshToken(user.getId(), getAuthentication(request.getEmail(), request.getUserPw()));
        return UserResponse.signin.builder()
                .email(request.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // access Token 재발급
    @Transactional
    public UserResponse.reissue reissue(UserRequest.reissue request){

        String accessToken = tokenProvider.reissue(request);

        return UserResponse.reissue.builder()
                .accessToken(accessToken)
                .build();
    }

    //logout
    @Transactional
    public UserResponse.logout logout(UserRequest.logout request){
        // 1. Access Token 검증
        if (!tokenProvider.validateToken(request.getAccessToken())) {
            throw new CustomException(Result.BAD_REQUEST);
        }

        // 2. Access Token 에서 User email 을 가져옵니다.
        Authentication authentication = tokenProvider.getAuthentication(request.getAccessToken());

        // 3. Redis 에서 해당 User email 로 저장된 Refresh Token 이 있는지 여부를 확인 후 있을 경우 삭제합니다.
        if (redisUtil.getData(authentication.getName()) != null) {
            // Refresh Token 삭제
            redisUtil.deleteData(authentication.getName());
        }

        // 4. 해당 Access Token 유효시간 가지고 와서 BlackList 로 저장하기
        Long expiration = tokenProvider.getExpiration(request.getAccessToken());
        redisUtil.setDataExpire(request.getAccessToken(), "logout", expiration);

        return UserResponse.logout.builder()
                .userId(Long.valueOf(authentication.getName()))
                .build();
    }

    //전체 유저 조회
    @Transactional(readOnly = true)
    public List<UserResponse.userAll> findAll(){
        List<User> all = userRepository.findAll();
        List<UserResponse.userAll> userAll = new ArrayList<>();

        for(User user: all){
            UserResponse.userAll a = UserResponse.userAll.builder()
                .email(user.getEmail())
                .name(user.getName())
                .phoneNum(user.getPhoneNum())
                .build();

            userAll.add(a);
        }
        return userAll;

        /*
        스트림으로
        List<User> all = userRepository.findDataProducts();

        return all.stream()
            .map(user -> UserResponse.userAll.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .phoneNum(user.getPhoneNum())
                    .build())
            .collect(Collectors.toList());
         */
    }

    // 좋아요 한 데이터 조회
    @Transactional
    public List<DataProductResponse.findHeartDataProducts> findHeartDataProducts(Long userId){
        User user = userRepository.findById(userId).orElseThrow(()-> new CustomException(Result.NOT_FOUND_USER));
        List<Heart> hearts = user.getHearts();

        List<DataProduct> dataProducts = new ArrayList<>();
        for(Heart heart: hearts){
            dataProducts.add(heart.getDataProduct());
        }

        List<DataProductResponse.findHeartDataProducts> response = new ArrayList<>();

        for(DataProduct dataProduct:dataProducts){
            ExampleImage exampleImage = dataProduct.getExampleImages().get(0);
            String imageUrl = exampleImage.getImageUrl();

            List<ProductCategory> productCategories = dataProduct.getProductCategories();
            List<String> categoryTitles = new ArrayList<>();
            for(ProductCategory productCategory : productCategories){
                String categoryTitle = productCategory.getCategory().getTitle();
                categoryTitles.add(categoryTitle);
            }

            DataProductResponse.findHeartDataProducts findHeartDataProducts = DataProductResponse.findHeartDataProducts
                    .builder()
                    .productId(dataProduct.getId())
                    .title(dataProduct.getTitle())
                    .price(dataProduct.getPrice())
                    .description(dataProduct.getDescription())
                    .createdAt(dataProduct.getCreatedAt())
                    .imageUrl(imageUrl)
                    .categoriesName(categoryTitles)
                    .build();
            response.add(findHeartDataProducts);
        }
        return response;
    }

    private void validateDuplicatedUserEmail(String userEmail) {
        Boolean existsByNickName = userRepository.existsByEmail(userEmail);
        if (existsByNickName) {
            throw new CustomException(Result.USERID_DUPLICATED);
        }
    }


    private Authentication getAuthentication(String email, String password) {
        //Authentication 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    //회원 가입시 권한을 ROLE_USER로 추가하는 DTO
    private User createEntityUserFromDto(UserRequest.signup request) {
        return User.builder()
                .email(request.getEmail())
                .userPw(passwordEncoder.encode(request.getUserPw()))
                .name(request.getName())
                .phoneNum(request.getPhoneNum())
                .authorities(getAuthorities())
                .build();
    }

    private static Set<Authority> getAuthorities() {
        return Collections.singleton(Authority.builder()
                .authorityName("ROLE_USER")
                .build());
    }

    public UserResponse.user findOne(Long id) {

        User user = userRepository.findById(id).get();
        return UserResponse.user.builder().id(user.getId()).build();
    }
}
