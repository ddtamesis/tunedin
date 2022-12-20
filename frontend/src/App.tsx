import React, { useState, Dispatch, SetStateAction, useEffect, useCallback } from 'react';
import GraphVis, { camtarg, getsortmethod, slidenum, sortshift, updatecamcenter } from './GraphVis'
import logo from './logo.svg';
import './styles/App.css';
import {firebaseConfig} from './private/firebaseconfig'
import * as d3 from 'd3';
import { signInWithGoogle } from './GoogleLogin';
import { SpotifyLoginButton} from './SpotifyAuth';
import { getFirestore, doc, setDoc, getDoc } from "firebase/firestore"; 
import { initializeApp } from "firebase/app";
import { updateuserdata } from './backendhandler';

// DONE TODOS:
//[DONE]: We need to be able to render a whole ton of circles.
//[DONE]: Add the actual parameters we want
//[DONE]: Change background color
//[DONE]: Add camera controls
//[DONE]: Make button fade in dynamically
//[DONE]: Make the sidebar change when you zoom in
//[DONE]: Add number displays in the sidebar
//[DONE]: Make a pretty logo and slap it in
//[DONE]: Add the current user
//[DONE]: Add matches and matches navigation
//[DONE]: Add variables for everything we're actually going to use
//[DONE]: Account for when the google user is not logged in but the spotify is already linked
//[DONE]: Make google thing vanish after you've logged in
//[DONE]: Make the website actually look the way we want it to
//[DONE]: FIX THE INFINITE LOOP PLEASE
//[DONE]: make a button to change the sorting parameter!
//[DONE]: Get frontend to communicate with backend
//[DONE]: Set curruser when you log in!
//[DONE]: Make current user index update when you log in! Edit the initdist functions
//[DONE]: Aria label the heck out of everything
//[DONE]: Re-render users once you've logged in fully!
//[DONE]: Fix long title formatting!
//[DONE]: Add a pretty gradient bar on the side to denote how things are being sorted from bottom to top
//[DONE]: Turn off the user outline circles when you aren't logged in
//[DONE]: If you can't find the current google user in the userlist(mocked data), consider making a separate screen?

//FRONTEND TODO:
//TODO: add a cute credits/about us section
//TODO: COMMENT EVERYTHING
//TODO: CLEAN UP AND TEST


// A function takes maxnum and randomly generates that many user coordinates across our screen.
// The general template for a set of "user coordinates" is as follows:
// [<user index number>, <user x position>, <user y position>]
// The x and y position are used and updated to make the user bubbles move across the screen,
// while the user index is used to identify each bubble with an actual user's data
export function initdist(num: number): Array<Array<number>>{
  let returnarr: Array<Array<number>> = new Array<Array<number>>();
  for(let i = 0; i < num; i++){
      // Add a randomly generated user coordinate to the stack
      returnarr.push([i,1500*(Math.random()-0.5),600*(Math.random()-0.5)])
  }
  return returnarr;
}

//This returns a promise that will only resolve when all users' data have been updated
async function setuserdata(userIDs:Array<string>, usersongparams: Map<number, Array<number>>,
  userdatastrings: Map<number, Array<string>>, matchesdata: Map<number, Array<Array<number>>>, 
  setusersongparams: ((map: Map<number, Array<number>>) => void), setuserdatastrings: ((map: Map<number, Array<string>>) => void), 
  setmatchesdata: ((map: Map<number, Array<Array<number>>>) => void)): Promise<void[]>{
  // console.log(googleuserid)
  const range: Array<number> = Array.from(Array(userIDs.length).keys())
  const promises: Array<Promise<void>> = range.map((i:number)=>{
      return updateuserdata(i, userIDs,usersongparams,userdatastrings,matchesdata, setusersongparams, setuserdatastrings, setmatchesdata)})
  return Promise.all(promises)
}

function getcurruserindex(userIDs: Array<string>,googleuser: string): number{
  if(userIDs.includes(googleuser)){
      return userIDs.indexOf(googleuser)
  }
  else{
    userunregistered = true;
    console.log(userIDs + " does not contain " + googleuser)
    return 0;
  }
}

function sawtooth(x: number): number{
  return Math.abs(((2*x) % 2)-1)
}

function aboutus(){
  return <div className="aboutustext" aria-label = "click for more info about tunedin">
    <svg width = "75" height = "50">
    <rect 
      key = "paramsortbutton"
      width = "75"
      height = "30"
      x= "0"
      y= "0"
      rx="5"
      ry="5"
      onClick={()=>{aboutusdisplay = true}}
      >
      </rect>
      <text className = "whitetext" x = "9" y = "22"
      onClick={()=>{aboutusdisplay = true}}>
      About
      </text>
    </svg>
  </div>
}

function aboutusinfo(){
  if(aboutusdisplay){
  return  <div className = "aboutusinfo" aria-label = "FILL THIS IN SOON">
    <svg width = "510" height = "400">
        <rect 
          key = "textbox"
          width = "500"
          height = "380"
          x= "5"
          y= "5"
          rx="5"
          ry="5"
          />
          <text className = "whitetext2" x = "20" y = "25"
          onClick={()=>{aboutusdisplay = false}}>
          {"[Click to close window]"}
          </text>

          <text className = "whitetext2" x = "20" y = "75">
          Tunedin is a musical social media app designed by Samantha Minars, 
          </text>

          <text className = "whitetext2" x = "20" y = "100">
          Denise Tamesis, Chance Emerson, and Dylan Lee.
          </text>

          <text className = "whitetext2" x = "20" y = "125">
          Find your perfect music match with Tunedin's sophisticated algorithm!
          </text>

          <text className = "whitetext2" x = "20" y = "150">
          Expand your music universe and keep up with your friends' latest jams.
          </text>

          <text className = "whitetext2" x = "20" y = "200">
          TUNE IN. YOU WIN.
          </text>

          <text className = "whitetext2" x = "20" y = "225">
          It's a win-win, take TUNEDIN for a spin.
          </text>

          <text className = "whitetext2" x = "20" y = "250">
          Tune out the din, tune in with TUNEDIN!
          </text>

          <text className = "whitetext2" x = "20" y = "275">
          You'll never not grin when tuning TUNEDIN in.
          </text>

          <text className = "whitetext2" x = "20" y = "300">
          FALL for TUNEDIN like a bowling pin!
          </text>

          <text className = "whitetext2" x = "20" y = "325">
          Your life wears thin. Repair your skin with TUNEDIN.
          </text>

          <text className = "whitetext2" x = "20" y = "350">
          Use TUNEDIN or forfeit your shins. Win and sin in the din in TUNEDIN.
          </text>

          <text className = "whitetext2" x = "20" y = "375">
          help me
          </text>
    </svg>
  </div>
  }
}

let userunregistered = false;

let aboutusdisplay = false;

function App() {
  const [CurrentGoogleUser, SetCurrentGoogleUser] = useState<string>("");
  const [spotifyLinked, setspotifyLinked] = useState<boolean>(false);
  const [usersloaded, setusersloaded] = useState<boolean>(false);
  const [fetchingusers, setfetchingusers] = useState<boolean>(false)
  const [usersongparams, setusersongparams] = useState<Map<number, Array<number>>>(new Map)
  const [userdatastrings, setuserdatastrings] = useState<Map<number, Array<string>>>(new Map)
  const [matchesdata, setmatchesdata] = useState<Map<number,Array<Array<number>>>> (new Map)
  const [Timer, setTimer] = useState<number>(0)
  const [userIDs, setuserIDs] = useState<Array<string>>([])
  const [CircleData, setCircleData] = useState<number[][]>([]);
  const [SortParameter, setSortParameter] = useState<number>(0);
  const [SortIndex, setSortIndex] = useState<number>(1);
  const [camcenter, Setcamcenter] = useState<number[]>([1,0,0]); //scale, position x, position y
  const [SelectIndex, setSelectIndex] = useState<number>(0);
  const [zoomval, Setzoomval] = useState<number>(0);
  const [zoomed, Setzoomed] = useState<boolean>(false);
  const [alltime, Setalltime] = useState<boolean>(false);
  const [curruserindex, Setcurruserindex] = useState<number>(0);

  const Speed = 10;

  useEffect(() => {
    const interval = setInterval(() => {
      if(CurrentGoogleUser != ""){
        // console.log("there is a google user " + CurrentGoogleUser.toString())
        if(!spotifyLinked){
          // console.log("spotify isn't linked")
          checkSpotifyLinked().then((result)=>{
            if(result == true){
            setspotifyLinked(result);
            }
          });
        }
      }
      else{
        // console.log("no current google user, only " + CurrentGoogleUser.toString())
      }
      //UPDATING USERS DISPLAYED
      setTimer((Timer + 0.001) % 1)
            if(!usersloaded){
              // console.log("Users aren't loaded yet but our google id is" + CurrentGoogleUser)
                if (!fetchingusers){
                    // console.log("we need to get stuff")
                    setfetchingusers(true)
                    fetch("http://localhost:3232/get-all-user-ids").then((respjson)=>{
                        respjson.json().then((respobj)=>{
                            const ids = respobj.ids
                            setuserIDs(ids)
                            setCircleData(initdist(ids.length))
                            fetch("http://localhost:3232/load-song-features").then(()=>{
                                fetch("http://localhost:3232/load-connections").then(()=>{
                                    setuserdata(ids, usersongparams, userdatastrings, matchesdata, setusersongparams, setuserdatastrings, setmatchesdata).then(()=>
                                    {
                                        setusersloaded(true)
                                    })
                                })
                            })
                        })
                    })
                }
            }
            if(usersloaded){
                // console.log("finding user " + googleuser + " in " + userIDs)
                // console.log("users are loaded with current user" + CurrentGoogleUser)
            Setcurruserindex(getcurruserindex(userIDs,CurrentGoogleUser))
            setCircleData(sortshift(CircleData, SortParameter, getsortmethod(SortIndex), Speed, spotifyLinked, curruserindex, usersongparams))
            Setcamcenter(updatecamcenter(camcenter,
            camtarg([4,CircleData[SelectIndex][1],CircleData[SelectIndex][2]],
                 [1,0,0],zoomed)))
             Setzoomval(1-((camcenter[0]-2)/2))
            document.documentElement.style.setProperty('--sidebar-mode', zoomval.toString());
            document.documentElement.style.setProperty('--timeslidermode', slidenum(alltime).toString());
            }


    }
    , 10);
     return () => clearInterval(interval);
})

  const app = initializeApp(firebaseConfig);
  const db = getFirestore(app);
    // check if the user's Spotify is linked
  async function checkSpotifyLinked() {
    const localUID = localStorage.getItem("UID")
    if (localUID != null) {
      let userDoc = await getDoc(doc(db, "users", localUID))
      let data = userDoc.data()
      console.log(data)
      // if the user rexists
      if (data != undefined) {
        // if the user spotify is linked
        if (data["refreshToken"] != "" && data["refreshToken"] != undefined) {
          console.log("there is a refresh token")
          return true;
        // if the user spotify is not linked
        } else {
          console.log("there was no refresh token detected")
          return false;
        }
      // if the user does not exist
      } else {
        console.log("this user has not logged into their google account yet")
        return false;
      }
    }
    else{
      console.log("local UID NULL")
    }
  }

  function hidebutton(): string{
    if(CurrentGoogleUser == ""){
      return "hidden"
    }
    else{
      return "spotifybutton"
    }
  }

  function showgoogleuserstring(){
    if(userunregistered){
      return ""
    }
    else{
      return CurrentGoogleUser
    }
  }

  function warningscreen(){
    if(userunregistered){
      return <svg className="warningscreen" width = "100%" height = "100%">
        {[0,1,2,3,4].map((x)=> {return <image href="https://i.ibb.co/8dYvrr8/Screen-Shot-2022-12-20-at-12-40-41-AM.png"
        x={0+800*sawtooth(3*(Timer+x/5))}
        y={0 + 275*sawtooth(2*(Timer+x/5))}
        />})}
        <image href="https://i.ibb.co/8dYvrr8/Screen-Shot-2022-12-20-at-12-40-41-AM.png"
        x={800*0.5}
        y={275*0.5}
        />
      </svg>
    }
  }

  return (
    <div className="App">
      <p className="App-header" aria-label = "App header">
        {/* {CurrentGoogleUser} */}
        {(CurrentGoogleUser == "") && 
        <button className="google-button" onClick = {()=>{let x = signInWithGoogle(SetCurrentGoogleUser)}} aria-label = "Click here to sign in with google">Sign in With Google</button>}
        <img className = "tuneinlogo" src="https://i.ibb.co/rFTJDTr/tuneinlogo2.png" aria-label = "Logo for the tunedin website"/>
      </p>
      {GraphVis(showgoogleuserstring(), spotifyLinked && !userunregistered, usersloaded, fetchingusers, usersongparams, userdatastrings, matchesdata, Timer,
       userIDs, CircleData, SortParameter, SortIndex, camcenter, SelectIndex, zoomval, zoomed, alltime, curruserindex,
       Setalltime, setSelectIndex, Setzoomed, setSortParameter, setSortIndex)}
      <div className = {hidebutton()} aria-label = "Click here to log in to spotify">
        <SpotifyLoginButton clientId={"213450855ac44f5aa842c2359939fded"} 
        redirectUri={'http://localhost:3000/callback/'} 
        clientSecret = {'9771ae6d19724806b33c585b57068127'} 
        setUser2 = {SetCurrentGoogleUser} 
        spotifyLinked = {spotifyLinked}
        setspotifyLinked = {setspotifyLinked}
        setusersloaded = {setusersloaded}
        setfetchingusers = {setfetchingusers}
        />
      </div>
      {aboutus()}
      {aboutusinfo()}
      {warningscreen()}
      {/* <p>{"Google id: " + CurrentGoogleUser}</p>
      <p>{"Spotify status: " + spotifyLinked}</p> */}
    </div>
  );
}

export default App;
