package com.bfrost.backend.post;

import com.bfrost.backend.club.Club;
import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.notification.NotificationService;
import com.bfrost.backend.post.dto.CreatePostRequest;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ReactionRepository reactionRepository;
    @Mock private SavedPostRepository savedPostRepository;
    @Mock private PollOptionRepository pollOptionRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private NotificationService notificationService;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, commentRepository, reactionRepository, savedPostRepository,
                pollOptionRepository, pollVoteRepository, userRepository, membershipRepository, clubRepository,
                notificationService);
    }

    @Test
    void createRejectsPollWithFewerThanTwoOptions() {
        UUID authorId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest(TargetType.USER_PAGE, authorId, PostType.POLL,
                "title", "body", null, null, null, List.of("Only one option"));

        assertThatThrownBy(() -> postService.create(req, authorId)).isInstanceOf(IllegalArgumentException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void createRejectsPollWithNoOptions() {
        UUID authorId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest(TargetType.USER_PAGE, authorId, PostType.POLL,
                "title", "body", null, null, null, null);

        assertThatThrownBy(() -> postService.create(req, authorId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsNonMemberPostingToClub() {
        UUID authorId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        CreatePostRequest req = new CreatePostRequest(TargetType.CLUB_PAGE, clubId, PostType.TEXT,
                "title", "body", null, null, null, null);
        when(membershipRepository.existsByClubIdAndUserId(clubId, authorId)).thenReturn(false);

        assertThatThrownBy(() -> postService.create(req, authorId)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deletePostRejectsNonAuthor() {
        UUID authorId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User author = User.builder().id(authorId).build();
        Post post = Post.builder().id(postId).author(author).targetType(TargetType.USER_PAGE)
                .targetId(authorId).postType(PostType.TEXT).body("hi").build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(postId, otherId)).isInstanceOf(ForbiddenException.class);
        verify(postRepository, never()).delete(any());
    }

    @Test
    void deletePostSucceedsForAuthor() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        User author = User.builder().id(authorId).build();
        Post post = Post.builder().id(postId).author(author).targetType(TargetType.USER_PAGE)
                .targetId(authorId).postType(PostType.TEXT).body("hi").build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePost(postId, authorId);

        verify(postRepository).delete(post);
    }

    @Test
    void getTargetPostsRejectsNonMemberOnPrivateClub() {
        UUID clubId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Club club = Club.builder().id(clubId).isPublic(false).build();
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(membershipRepository.existsByClubIdAndUserId(clubId, strangerId)).thenReturn(false);

        assertThatThrownBy(() -> postService.getTargetPosts(TargetType.CLUB_PAGE, clubId, strangerId, null, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getTargetPostsThrowsWhenClubMissing() {
        UUID clubId = UUID.randomUUID();
        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getTargetPosts(TargetType.CLUB_PAGE, clubId, null, null, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void voteOnPollRejectsOptionFromAnotherPost() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID otherPostId = UUID.randomUUID();
        Post otherPost = Post.builder().id(otherPostId).build();
        PollOption option = PollOption.builder().id(UUID.randomUUID()).post(otherPost).build();
        when(pollVoteRepository.countVotesByUserOnPost(postId, userId)).thenReturn(0L);
        when(pollOptionRepository.findById(option.getId())).thenReturn(Optional.of(option));

        assertThatThrownBy(() -> postService.votePoll(postId, option.getId(), userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void voteOnPollRejectsSecondVote() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(pollVoteRepository.countVotesByUserOnPost(postId, userId)).thenReturn(1L);

        assertThatThrownBy(() -> postService.votePoll(postId, UUID.randomUUID(), userId))
                .isInstanceOf(ConflictException.class);
    }
}
