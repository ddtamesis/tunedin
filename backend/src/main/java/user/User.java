package user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import kdtree.DistanceSorter;
import kdtree.KdTree;
import kdtree.KdTreeNode;
import kotlin.jvm.Transient;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.AudioFeatures;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.data.player.GetCurrentUsersRecentlyPlayedTracksRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetAudioFeaturesForTrackRequest;
import server.Constants;
import song.Song;
import song.SongLibrary;

/**
 * Class representing an individual TuneIn user, which houses essential user-specific information.
 */
public class User implements KdTreeNode, Cloneable {
  private String userId;
  private String displayName;
  private String refreshToken;
  private int membershipLength;
  private Song currentSong;
  private String[] connections;
  private double[] historicalSongPoint;
  private String[] historicalConnections;
  private transient SongLibrary songLibrary;

  public User(
      String userId,
      String displayName,
      String refreshToken,
      int membershipLength,
      Song currentSong,
      String[] connections,
      double[] historicalSongPoint,
      String[] historicalConnections) {
    this.userId = userId;
    this.displayName = displayName;
    this.refreshToken = refreshToken;
    this.membershipLength = membershipLength;
    this.currentSong = currentSong;
    this.connections = connections;
    this.historicalSongPoint = historicalSongPoint;
    this.historicalConnections = historicalConnections;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return this.membershipLength == user.membershipLength && this.userId.equals(user.userId)
        && this.displayName.equals(user.displayName) && Objects.equals(this.refreshToken,
        user.refreshToken) && this.currentSong.equals(user.currentSong) && Arrays.equals(
        this.connections, user.connections) && Arrays.equals(this.historicalSongPoint,
        user.historicalSongPoint) && Arrays.equals(this.historicalConnections,
        user.historicalConnections) && Objects.equals(this.songLibrary, user.songLibrary);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(userId, membershipLength);
    result = 31 * result + currentSong.hashCode();
    result = 31 * result + Arrays.hashCode(connections);
    result = 31 * result + Arrays.hashCode(historicalSongPoint);
    result = 31 * result + Arrays.hashCode(historicalConnections);
    return result;
  }

  public String getUserId() {
    return this.userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public int getMembershipLength() {
    return this.membershipLength;
  }

  public void setMembershipLength(int membershipLength) {
    this.membershipLength = membershipLength;
  }

  public Song getCurrentSong() {
    return currentSong;
  }

  public void setCurrentSong(Song currentSong) {
    this.currentSong = currentSong;
  }

  public String[] getConnections() {
    return this.connections.clone();
  }

  public void setConnections(String[] connections) {
    this.connections = connections;
  }

  public double[] getHistoricalSongPoint() {
    return this.historicalSongPoint.clone();
  }

  public void setHistoricalSongPoint(double[] historicalSongPoint) {
    this.historicalSongPoint = historicalSongPoint;
  }

  public String[] getHistoricalConnections() {
    return this.historicalConnections.clone();
  }

  public void setHistoricalConnections(String[] historicalConnections) {
    this.historicalConnections = historicalConnections;
  }

  public void setSongLibrary(SongLibrary songLibrary) {
    this.songLibrary = songLibrary;
  }

  @Override
  public User clone() {
    try {
      User clone = (User) super.clone();
      // TODO: copy mutable state here, so the clone can't change the internals of the original
      clone.setUserId(this.getUserId());
      clone.setMembershipLength(this.getMembershipLength());
      clone.setCurrentSong(this.getCurrentSong());
      clone.setConnections(this.getConnections());
      clone.setHistoricalSongPoint(this.getHistoricalSongPoint());
      clone.setHistoricalConnections(this.getHistoricalConnections());
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public boolean isMocked() {
    return this.songLibrary != null;
  }

  public boolean hasRefreshToken() {
    Set<String> conditions = new HashSet<>(Arrays.asList(null, "", "\"\"", " "));
    return !conditions.contains(this.getRefreshToken());
  }

  public Song getMostRecentSong()
      throws IOException, ParseException, ExecutionException, InterruptedException,
      SpotifyWebApiException {
    // if spotify has been linked:
    if (this.hasRefreshToken()) {
      System.out.println("Refresh token present: " + this.getRefreshToken());
      String authToken = this.getAuthToken();
      SpotifyApi spotifyApi =
          new SpotifyApi.Builder()
              .setClientId(Constants.CLIENT_ID)
              .setClientSecret(Constants.CLIENT_SECRET)
              .setAccessToken(authToken)
              .build();

      GetCurrentUsersRecentlyPlayedTracksRequest getCurrentUsersRecentlyPlayedTracksRequest =
          spotifyApi.getCurrentUsersRecentlyPlayedTracks().limit(1).build();
      System.out.println("Recently Played Request made. Will now execute...");
      PagingCursorbased<PlayHistory> playHistoryPagingCursorbased =
          getCurrentUsersRecentlyPlayedTracksRequest.execute();
      System.out.println("Successfully executed.");

      PlayHistory[] playHistories = playHistoryPagingCursorbased.getItems();
      System.out.println("Date/Time Played: " + playHistories[0].getPlayedAt());
      TrackSimplified track = playHistories[0].getTrack();

      String title = track.getName();
      String id = track.getId();
      // artists
      List<String> artists = new ArrayList<>();
      ArtistSimplified[] artistsSimp = track.getArtists();
      for (ArtistSimplified artist : artistsSimp) {
        artists.add(artist.getName());
      }
      // features
      GetAudioFeaturesForTrackRequest getAudioFeaturesForTrackRequest =
          spotifyApi.getAudioFeaturesForTrack(id).build();
      AudioFeatures audioFeatures = getAudioFeaturesForTrackRequest.execute();

      double[] features = new double[6];
      features[0] = audioFeatures.getAcousticness();
      features[1] = audioFeatures.getDanceability();
      features[2] = audioFeatures.getEnergy();
      features[3] = audioFeatures.getInstrumentalness();
      features[4] = audioFeatures.getSpeechiness();
      features[5] = audioFeatures.getValence();

      return new Song(this.getUserId(), title, id, artists, features);
    } else if (this.isMocked()) {
      Song newSong = this.songLibrary.getRandom();
      newSong.setUserId(this.getUserId());
      return newSong;
    } else {
      // perform get the last successful song update
      return this.getCurrentSong();
    }
  }

  /**
   * Helper method that takes in a user's refresh token and makes a call to the Spotify API to
   * return a valid access token
   *
   * @return access token
   */
  private String getAuthToken() throws SpotifyWebApiException, ParseException {
    try {
      SpotifyApi spotifyApi =
          new SpotifyApi.Builder()
              .setClientId(Constants.CLIENT_ID)
              .setClientSecret(Constants.CLIENT_SECRET)
              .setRefreshToken(this.getRefreshToken())
              .build();
      AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest =
          spotifyApi.authorizationCodeRefresh().build();
      AuthorizationCodeCredentials authorizationCodeCredentials =
          authorizationCodeRefreshRequest.execute();
      return authorizationCodeCredentials.getAccessToken();
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      throw new RuntimeException(e);
    } catch (ForbiddenException e) {
      System.out.println("Forbidden exception: " + e.getMessage());
      throw new ForbiddenException(e.getMessage());
    }
  }

  public void updateHistoricalSongPoint(double[] newSongPoint) {
    // current average point + [(new point - current average point) / membershipLength]
    this.membershipLength++; // n increases by 1 because a new point is being added to average
    if (this.membershipLength == 1) {
      this.setHistoricalSongPoint(this.getCurrentSong().getPoint());
    } else {
      double[] newHistoricalSongPoint = new double[6];
      for (int i = 0; i < newSongPoint.length; i++) {
        newHistoricalSongPoint[i] = newSongPoint[i] - this.getHistoricalSongPoint()[i];
        newHistoricalSongPoint[i] = newHistoricalSongPoint[i] / this.getMembershipLength();
        newHistoricalSongPoint[i] = newHistoricalSongPoint[i] + this.getHistoricalSongPoint()[i];
      }
      this.setHistoricalSongPoint(newHistoricalSongPoint);
    }
  }

  @Override
  public double[] getPoint() {
    return this.getHistoricalSongPoint();
  }

  @Override
  public int getDimension() {
    return this.getHistoricalSongPoint().length;
  }

  @Override
  public double euclideanDistance(KdTreeNode node) {
    double[] currentVals = this.getHistoricalSongPoint();
    double[] targetVals = node.getPoint();
    double sum = 0;
    for (int i = 0; i < currentVals.length; i++) {
      sum += Math.pow(currentVals[i] - targetVals[i], 2);
    }
    return Math.sqrt(sum);
  }

  public String[] findConnections(KdTree<Song> songTree) {
    HashSet<Song> excluded = new HashSet<Song>();
    excluded.add(this.getCurrentSong());
    PriorityQueue<Song> connectionsQueue = songTree.kdTreeSearch(
        "neighbors", 5, this.getCurrentSong(), new DistanceSorter(this.getCurrentSong()), excluded);
    // reverse order of connections for array bc queue is in decreasing order of distance
    String[] connections = new String[5];
    int i = connectionsQueue.size() - 1;
    for (Song song : connectionsQueue) {
      connections[i] = song.getUserId();
      i--;
    }
    return connections;
  }

  public String[] findHistoricalConnections(KdTree<User> userTree) {
    HashSet<User> excluded = new HashSet<User>();
    excluded.add(this);
    PriorityQueue<User> connectionsQueue = userTree.kdTreeSearch(
        "neighbors", 5, this, new DistanceSorter(this), excluded);
    // reverse order of connections for array bc queue is in decreasing order of distance
    String[] connections = new String[5];
    int i = connectionsQueue.size() - 1;
    for (User user : connectionsQueue) {
      connections[i] = user.getUserId();
      i--;
    }
    return connections;
  }
}
