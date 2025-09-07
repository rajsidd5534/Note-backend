package com.notesapp.notesbackend.controller;

import com.notesapp.notesbackend.model.Note;
import com.notesapp.notesbackend.model.User;
import com.notesapp.notesbackend.repository.NoteRepository;
import com.notesapp.notesbackend.repository.UserRepository;
import com.notesapp.notesbackend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired private NoteRepository noteRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private JwtUtil jwtUtil;

    // ✅ Common helper method to extract JWT
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        return authHeader.substring(7).trim(); // Remove "Bearer "
    }

    @PostMapping
    public Note create(@RequestBody Map<String, String> body,
                       @RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Note note = new Note();
        note.setTitle(body.get("title"));
        note.setContent(body.get("content"));
        note.setUser(user);

        return noteRepo.save(note);
    }

    @GetMapping
    public List<Note> getAll(@RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);
        return noteRepo.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Note get(@PathVariable Long id,
                    @RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);

        Note note = noteRepo.findById(id).orElseThrow();
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to this note");
        }
        return note;
    }

    @PutMapping("/{id}")
    public Note update(@PathVariable Long id,
                       @RequestBody Map<String, String> body,
                       @RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);

        Note note = noteRepo.findById(id).orElseThrow();
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this note");
        }

        note.setTitle(body.get("title"));
        note.setContent(body.get("content"));
        return noteRepo.save(note);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id,
                                      @RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);

        Note note = noteRepo.findById(id).orElseThrow();
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this note");
        }

        noteRepo.delete(note);
        return Map.of("message", "Deleted");
    }

    @GetMapping("/share/{shareId}")
    public Note getShared(@PathVariable String shareId) {
        return noteRepo.findByShareId(shareId)
                .orElseThrow(() -> new RuntimeException("Shared note not found"));
    }

    @PostMapping("/share/{id}")
    public Map<String, String> shareNote(@PathVariable Long id,
                                         @RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        Long userId = jwtUtil.extractUserId(token);

        Note note = noteRepo.findById(id).orElseThrow();
        if (!note.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to share this note");
        }

        // If note already has a shareId, reuse it, otherwise generate a new one
        if (note.getShareId() == null || note.getShareId().isEmpty()) {
            String shareId = java.util.UUID.randomUUID().toString();
            note.setShareId(shareId);
            noteRepo.save(note);
        }

        return Map.of("shareUrl", "https://note-frontent.vercel.app/" + note.getShareId());
    }
}
