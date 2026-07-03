package com.bfrost.backend.post;

import com.bfrost.backend.club.ClubRepository;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.CursorPage;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import com.bfrost.backend.post.dto.CommentDto;
import com.bfrost.backend.post.dto.CreatePostRequest;
import com.bfrost.backend.post.dto.PollOptionDto;
import com.bfrost.backend.post.dto.PostDto;
import com.bfrost.backend.user.User;
import com.bfrost.backend.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;


    @Transactional
    public PostDto create(CreatePostRequest req, UUID authorId) {
        if (req.targetType() == TargetType.CLUB_PAGE
                && !membershipRepository.existsByClubIdAndUserId(req.targetId(), authorId)) {
            throw new ForbiddenException("You must be a member of this club to post");
        }
        User author = userRepository.getReferenceById(authorId);
        Post post = Post.builder()
                .author(author)
                .targetType(req.targetType())
                .targetId(req.targetId())
                .postType(req.postType())
                .title(req.title())
                .body(req.body())
                .mediaUrl(req.mediaUrl())
                .linkUrl(req.linkUrl())
                .channel(req.channel())
                .build();
        postRepository.save(post);

        if (req.postType() == PostType.POLL && req.pollOptions() != null) {
            for (int i = 0; i < req.pollOptions().size(); i++) {
                PollOption opt = PollOption.builder()
                    .post(post)
                    .optionText(req.pollOptions().get(i))
                    .position(i)
                    .build();
                pollOptionRepository.save(opt);
            }
        }

        return buildDto(post, authorId);
    }

    @Transactional(readOnly = true)
    public PostDto getById(UUID postId, UUID currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return buildDto(post, currentUserId);
    }

    @Transactional(readOnly = true)
    public CursorPage<PostDto> getFeed(UUID userId, String cursorStr, int size) {
        var cursor = parseCursor(cursorStr);
        List<Post> posts = postRepository.findFeedForUser(userId, cursor.createdAt(), cursor.id(), size + 1);
        return CursorPage.of(posts.stream().map(p -> buildDto(p, userId)).toList(), size + 1, dto -> encodeCursor(dto.createdAt(), dto.id()));
    }

    @Transactional(readOnly = true)
    public CursorPage<PostDto> getTargetPosts(TargetType targetType, UUID targetId, UUID currentUserId, String cursorStr, int size) {
        var cursor = parseCursor(cursorStr);
        List<Post> posts = postRepository.findByTargetWithCursor(targetType, targetId, cursor.createdAt(), cursor.id(), size + 1);
        return CursorPage.of(posts.stream().map(p -> buildDto(p, currentUserId)).toList(), size + 1, dto -> encodeCursor(dto.createdAt(), dto.id()));
    }

    @Transactional
    public void react(UUID postId, UUID userId, ReactionType type) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        Optional<Reaction> existing = reactionRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            Reaction r = existing.get();
            if (r.getReactionType() == type) {
                reactionRepository.delete(r);
                if (type == ReactionType.LIKE) post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                else post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
            } else {
                if (type == ReactionType.LIKE) {
                    post.setLikeCount(post.getLikeCount() + 1);
                    post.setDislikeCount(Math.max(0, post.getDislikeCount() - 1));
                } else {
                    post.setDislikeCount(post.getDislikeCount() + 1);
                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                }
                r.setReactionType(type);
                reactionRepository.save(r);
            }
        } else {
            User user = userRepository.getReferenceById(userId);
            reactionRepository.save(Reaction.builder().post(post).user(user).reactionType(type).build());
            if (type == ReactionType.LIKE) post.setLikeCount(post.getLikeCount() + 1);
            else post.setDislikeCount(post.getDislikeCount() + 1);

        }
    }

    @Transactional
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (!post.getAuthor().getId().equals(userId)) throw new ForbiddenException("Cannot delete another user's post");
        postRepository.delete(post);
    }

    @Transactional
    public CommentDto comment(UUID postId, UUID authorId, String body) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        User author = userRepository.getReferenceById(authorId);
        Comment c = Comment.builder().post(post).author(author).body(body).build();
        commentRepository.save(c);
        post.setCommentCount(post.getCommentCount() + 1);
        return CommentDto.from(c);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(UUID postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(CommentDto::from)
                .toList();
    }

    @Transactional
    public void votePoll(UUID postId, UUID optionId, UUID userId) {
        if (pollVoteRepository.countVotesByUserOnPost(postId, userId) > 0) {
            throw new ConflictException("Already voted on this poll");
        }
        PollOption option = pollOptionRepository.findById(optionId)
            .orElseThrow(() -> new ResourceNotFoundException("Option not found"));
        if (!option.getPost().getId().equals(postId)) throw new IllegalArgumentException("Option does not belong to this post");
        User user = userRepository.getReferenceById(userId);
        pollVoteRepository.save(PollVote.builder().option(option).user(user).build());
        option.setVoteCount(option.getVoteCount() + 1);
    }

    private PostDto buildDto(Post post, UUID currentUserId) {
        String reaction = null;
        boolean saved = false;
        List<PollOptionDto> options = List.of();
        if (currentUserId != null) {
            reaction = reactionRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                .map(r -> r.getReactionType().name()).orElse(null);
            saved = savedPostRepository.existsByPostIdAndUserId(post.getId(), currentUserId);
        }
        if (post.getPostType() == PostType.POLL) {
            List<UUID> voted = currentUserId != null
                ? pollOptionRepository.findVotedOptionIdsByUserAndPost(currentUserId, post.getId())
                : List.of();
            Set<UUID> votedSet = new HashSet<>(voted);
            options = post.getPollOptions().stream()
                .map(o -> PollOptionDto.from(o, votedSet.contains(o.getId())))
                .toList();
        }

        String targetName;
        String targetSlug;
        if (post.getTargetType() == TargetType.CLUB_PAGE) {
            var club = clubRepository.findById(post.getTargetId()).orElse(null);
            targetName = club != null ? club.getName() : "Club";
            targetSlug = club != null ? club.getSlug() : post.getTargetId().toString();
        } else {
            targetName = post.getAuthor().getDisplayName();
            targetSlug = post.getAuthor().getUsername();
        }
        return PostDto.from(post, reaction, saved, options, targetName, targetSlug);
    }

    private record Cursor(Instant createdAt, UUID id) {}

    private Cursor parseCursor(String cursorStr) {
        if (cursorStr == null) return new Cursor(Instant.now(), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursorStr));
            String[] parts = decoded.split("\\|");
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            return new Cursor(Instant.now(), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        }
    }

    private String encodeCursor(Instant createdAt, UUID id) {
        return Base64.getUrlEncoder().encodeToString((createdAt.toString() + "|" + id).getBytes());
    }
}
