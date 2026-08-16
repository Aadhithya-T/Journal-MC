import React, { useRef, useEffect } from "react";
import { useBackground } from "../../context/BackgroundContext";

export function VideoBackground() {
  const { currentBackground, isMuted } = useBackground();
  const videoRef = useRef(null);

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.load();
      videoRef.current.play().catch((err) => {
        console.log("Autoplay check:", err);
      });
    }
  }, [currentBackground.id]);

  return (
    <div className="mc-video-bg-container">
      <video
        ref={videoRef}
        className="mc-video-bg-player"
        autoPlay
        loop
        muted={isMuted}
        playsInline
        preload="auto"
        key={currentBackground.id}
      >
        <source src={currentBackground.src} type="video/mp4" />
        Your browser does not support HTML5 video.
      </video>

      {/* Minecraft Vignette Overlay */}
      <div className="mc-video-vignette" />
    </div>
  );
}
